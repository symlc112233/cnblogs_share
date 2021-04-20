package largeFloat;

/*
ultra-high accuracy floating-point calculation utility

accuracy: 32*length bits
maximum exponent: 2^60, or 10^18
operations: + - * / exp log

author: bubble_song
*/

public class LFloat
{
	public boolean negative;
	public long expo;
	public int length;
	public long[] chunk;
	public int state;//0: normal, 1: infinite, -1:small 2: nan;
	
	public static final long maskL = 0x00000000ffffffffl;
	public static final long maskH = 0xffffffff00000000l;
	
	public static final long expoL = -0x1000000000000000l;
	public static final long expoH = +0x1000000000000000l;
	
	public LFloat(int _length)
	{
		length = _length;
		chunk = new long[_length + 1];
		negative = false;
		state = 2;
	}
	
	public LFloat(int _length, double num)
	{
		long a = Double.doubleToLongBits(num);
		length = _length;
		chunk = new long[_length + 1];
		negative = (a < 0);
		expo = ((a & 0x7ff0000000000000l) >>> 52) - 1023;
		chunk[1] = ((a & 0x000fffffffffffffl) >>> 20);
		chunk[2] = ((a & 0x00000000000fffffl) << 12);
		
		if(expo == -1023)
		{
			chunk[0] = 0x0;
			expo = -1022;
			state = 0;
		}
		else
		{
			chunk[0] = 0x1;
			state = 0;
		}
		
		normalize();
	}
	
	public LFloat(int _length, String str)
	{
		length = _length;
		
		str = str.toUpperCase();
		negative = false;
		long exp = 0;
		String dec = "";
		if(str.startsWith("+"))
		{str = str.substring(1);}
		else if(str.startsWith("-"))
		{
			negative = true;
			str = str.substring(1);
		}
		if(str.equalsIgnoreCase("NAN"))
		{
			state = 2;
			return;
		}
		else if(str.equalsIgnoreCase("INF"))
		{
			state = 1;
			return;
		}
		
		int a = str.indexOf(".");
		int b = str.indexOf("E");
		if(b > 0)
		{
			String exponent = str.substring(b + 1);
			exp = +1;
			if(exponent.startsWith("+"))
			{exponent = exponent.substring(1);}
			else if(exponent.startsWith("-"))
			{
				exp = -1;
				exponent = exponent.substring(1);
			}
			if(exponent.length() > 16)
			{throw new RuntimeException("LFloat -> input exponent too large!");}
			else
			{exp *= Long.parseLong(exponent);}
			
			if(a >= 0)
			{
				exp += a - 1;
				dec = str.substring(0, a) + str.substring(a + 1, b);
			}
			else
			{
				exp += b - 1;
				dec = str.substring(0, b);
			}
		}
		else
		{
			if(a >= 0)
			{
				exp = a - 1;
				dec = str.substring(0, a) + str.substring(a + 1);
			}
			else
			{
				exp = str.length() - 1;
				dec = str;
			}
		}
		while(dec.length() > 1 && dec.startsWith("0"))
		{
			exp -= 1;
			dec = dec.substring(1);
		}
		
		long[] decl = new long[dec.length()];
		for(int i = 0; i < dec.length(); i++)
		{
			decl[i] = Long.parseLong(dec.substring(i, i + 1));
		}
		
		long[] result = new long[length + 2];
		result[0] = decl[0];
		for(int i = 1; i < result.length; i++)
		{
			long carry = 0l;
			for(int j = decl.length - 1; j > 0; j--)
			{
				long sum = decl[j] * 0x100000000l + carry;
				carry = sum / 10l;
				decl[j] = sum % 10l;
			}
			result[i] = carry;
		}
		
		LFloat exp10 = new LFloat(length + 1, exp);
		LFloat aln10 = log(new LFloat(length + 1, 10)).mul(exp10);
		LFloat ln2 = log(new LFloat(length + 1, 2));
		long exp2 = aln10.div(ln2).toLong();
		
		LFloat remain = exp(aln10.sub(ln2.mul(new LFloat(length + 1, exp2))));
		
		long[] chunkl = chunkMul(length + 1, remain.chunk, result);
		
		expo = exp2 + remain.expo;
		state = 0;
		chunk = new long[length + 1];
		for(int i = 0; i <= length; i++)
		{chunk[i] = chunkl[i];}
		
		normalize();
	}
	
	public LFloat num(String str)
	{
		return new LFloat(length, str);
	}
	
	public LFloat num(double num)
	{
		return new LFloat(length, num);
	}
	
	public LFloat add(LFloat lf)
	{
		LFloat ans = copy();
		ans.selfAdd(lf);
		return ans;
	}
	
	public void selfAdd(LFloat lf)
	{
		if(state == 2 || lf.state == 2 || (state == 1 && lf.state == 1 && (negative != lf.negative)))
		{
			state = 2;
			return;
		}
		if(state == 1 && lf.state == 1 && (negative == lf.negative))
		{return;}
		if(state == 1 && lf.state < 1)
		{return;}
		if(state < 1 && lf.state == 1)
		{
			state = 1;
			negative = lf.negative;
			return;
		}
		
		LFloat a1 = lf.copy();
		
		long b1 = expo;
		long b2 = a1.expo;
		
		if(b1 > b2)
		{
			chunkShr(length, a1.chunk, b1 - b2);
			if(negative == a1.negative)
			{chunkAdd(length, chunk, a1.chunk);}
			else
			{chunkSub(length, chunk, a1.chunk);}
		}
		else if(b1 < b2)
		{
			chunkShr(length, chunk, b2 - b1);
			if(negative == a1.negative)
			{chunkAdd(length, a1.chunk, chunk);}
			else
			{chunkSub(length, a1.chunk, chunk);}
			chunk = a1.chunk;
			negative = a1.negative;
			expo = a1.expo;
		}
		else
		{
			if(negative == a1.negative)
			{chunkAdd(length, chunk, a1.chunk);}
			else
			{
				if(chunkAboveEqual(length, chunk, a1.chunk) == true)
				{chunkSub(length, chunk, a1.chunk);}
				else
				{
					chunkSub(length, a1.chunk, chunk);
					chunk = a1.chunk;
					negative = a1.negative;
					expo = a1.expo;
				}
			}
		}
		normalize();
	}
	
	public LFloat sub(LFloat lf)
	{
		LFloat ans = copy();
		ans.selfSub(lf);
		return ans;
	}
	
	public void selfSub(LFloat lf)
	{
		if(state == 2 || lf.state == 2 || (state == 1 && lf.state == 1 && (negative == lf.negative)))
		{
			state = 2;
			return;
		}
		if(state == 1 && lf.state == 1 && (negative != lf.negative))
		{return;}
		if(state == 1 && lf.state < 1)
		{return;}
		if(state < 1 && lf.state == 1)
		{
			state = 1;
			negative = !(lf.negative);
			return;
		}
		
		LFloat a1 = lf.copy();
		
		long b1 = expo;
		long b2 = a1.expo;
		
		if(b1 > b2)
		{
			chunkShr(length, a1.chunk, b1 - b2);
			if(negative != a1.negative)
			{chunkAdd(length, chunk, a1.chunk);}
			else
			{chunkSub(length, chunk, a1.chunk);}
		}
		else if(b1 < b2)
		{
			chunkShr(length, chunk, b2 - b1);
			if(negative != a1.negative)
			{chunkAdd(length, a1.chunk, chunk);}
			else
			{chunkSub(length, a1.chunk, chunk);}
			chunk = a1.chunk;
			negative = !a1.negative;
			expo = a1.expo;
		}
		else
		{
			if(negative != a1.negative)
			{chunkAdd(length, chunk, a1.chunk);}
			else
			{
				if(chunkAboveEqual(length, chunk, a1.chunk) == true)
				{chunkSub(length, chunk, a1.chunk);}
				else
				{
					chunkSub(length, a1.chunk, chunk);
					chunk = a1.chunk;
					negative = !a1.negative;
					expo = a1.expo;
				}
			}
		}
		normalize();
	}
	
	public LFloat mul(LFloat lf)
	{
		LFloat ans = new LFloat(length);
		
		if(state == 2 || lf.state == 2 || (state == -1 && lf.state == 1) || (state == 1 && lf.state == -1))
		{
			ans.state = 2;
		}
		else if(state == 1 || lf.state == 1)
		{
			ans.state = 1;
			ans.negative = negative ^ lf.negative;
		}
		else
		{
			ans.expo = expo + lf.expo;
			ans.chunk = chunkMul(length, chunk, lf.chunk);
			ans.negative = negative ^ lf.negative;
			ans.state = 0;
		}
		
		ans.normalize();
		return ans;
	}
	
	public LFloat div(LFloat lf)
	{
		LFloat ans = new LFloat(length);
		
		if(state == 2 || lf.state == 2 || (state == 1 && lf.state == 1) || (state == -1 && lf.state == -1))
		{
			ans.state = 2;
			return ans;
		}
		else if(state == 1)
		{
			ans.state = 1;
			ans.negative = negative ^ lf.negative;
			return ans;
		}
		else if(lf.state == 1)
		{
			ans.state = -1;
			ans.expo = expoL;
			chunkClear(length, ans.chunk);
			ans.negative = false;
			return ans;
		}
		else
		{
		
			ans.expo = expo - lf.expo;
			ans.chunk = new long[length + 1];
			ans.negative = negative ^ lf.negative;
			ans.state = 0;
			
			long[] a1 = copy().chunk;
			long[] a2 = lf.chunk;
			for(int i = 31; i < (length + 1) * 32; i++)
			{
				if(chunkAboveEqual(length, a1, a2) == true)
				{
					chunkSub(length, a1, a2);
					ans.chunk[i / 32] |= 0x80000000l >> (i % 32);
				}
				chunkShl(length, a1, 1);
			}
		}
		
		ans.normalize();
		return ans;
	}
	
	public void normalize()
	{
		if(state == 0)
		{
			long h = chunkHighestBit(length, chunk);
			if(h == (length + 1) * 32)
			{
				state = -1;
				negative = false;
				expo = expoL;
				return;
			}
			chunkShr(length, chunk, 31 - h);
			expo += 31 - h;
			
			if(expo < expoL)
			{
				state = -1;
				chunkShr(length, chunk, expoL - expo);
				expo = expoL;
				if(chunkHighestBit(length, chunk) == (length + 1) * 32)
				{
					state = -1;
					negative = false;
					expo = expoL;
				}
			}
			else if(expo > expoH)
			{
				state = 1;
				expo = 0l;
			}
		}
		else if(state == -1)
		{
			long h = chunkHighestBit(length, chunk);
			if(h < 31)
			{
				chunkShr(length, chunk, 31 - h);
				expo += 31 - h;
			}
			if(expo > expoL)
			{state = 0;}
		}
	}
	
	public static long chunkHighestBit(int _length, long[] chunk)
	{
		for(int i = 0; i <= _length; i++)
		{
			if(chunk[i] != 0)
			{
				for(int j = 0; j < 32; j++)
				{
					if((chunk[i] & (0x80000000l >> j)) != 0l)
					{
						return 32 * i + j;
					}
				}
			}
		}
		return (_length + 1) * 32;
	}
	
	public static boolean chunkAbove(int _length, long[] c1, long[] c2)
	{
		for(int i = 0; i <= _length; i++)
		{
			if(c1[i] < c2[i])
			{return false;}
			else if(c1[i] > c2[i])
			{return true;}
		}
		return false;
	}
	
	public static boolean chunkAboveEqual(int _length, long[] c1, long[] c2)
	{
		for(int i = 0; i <= _length; i++)
		{
			if(c1[i] < c2[i])
			{return false;}
			else if(c1[i] > c2[i])
			{return true;}
		}
		return true;
	}
	
	public static void chunkAdd(int _length, long[] c1, long[] c2)
	{
		long count = 0;
		for(int i = _length; i >= 0; i--)
		{
			long sum = c1[i] + c2[i] + count;
			c1[i] = sum & maskL;
			count = (sum >>> 32) & 0x1l;
		}
	}
	
	public static void chunkSub(int _length, long[] c1, long[] c2)
	{
		long count = 0;
		for(int i = _length; i >= 0; i--)
		{
			long sum = c1[i] - c2[i] - count;
			c1[i] = sum & maskL;
			count = (sum >>> 32) & 0x1l;
		}
	}
	
	public static long[] chunkMul(int _length, long[] c1, long[] c2)
	{
		long[] ans = new long[_length + 1];
		for(int i = _length; i > 0; i--)
		{
			long count = (c1[i] * c2[_length - i + 1]) >>> 32;
			for(int j = _length - i; j >= 0; j--)
			{
				long sum = c1[i] * c2[j] + (ans[i + j] + count);
				ans[i + j] = sum & maskL;
				count = sum >>> 32;
			}
			ans[i - 1] += count;
		}
		long count = 0;
		for(int j = _length; j >= 0; j--)
		{
			long sum = c1[0] * c2[j] + (ans[j] + count);
			ans[j] = sum & maskL;
			count = sum >>> 32;
		}
		return ans;
	}
	
	public static void chunkShl(int _length, long[] c1, long digit)
	{
		if(digit < 0l)
		{chunkShr(_length, c1, -digit);}
		else if(digit >= (_length + 1) * 32)
		{chunkClear(_length, c1);}
		else
		{
			int a = (int)digit / 32;
			int b = (int)digit % 32;
			for(int i = 0; i < _length - a; i++)
			{c1[i] = ((c1[i + a] << b) | (c1[i + a + 1] >> (32 - b))) & maskL;}
			c1[_length - a] = (c1[_length] << b) & maskL;
			for(int i = _length; i > _length - a; i--)
			{c1[i] = 0l;}
		}
	}
	
	public static void chunkShr(int _length, long[] c1, long digit)
	{
		if(digit < 0l)
		{chunkShl(_length, c1, -digit);}
		else if(digit >= (_length + 1) * 32)
		{chunkClear(_length, c1);}
		else
		{
			int a = (int)digit / 32;
			int b = (int)digit % 32;
			for(int i = _length; i > a; i--)
			{c1[i] = ((c1[i - a] >> b) | (c1[i - a - 1] << (32 - b))) & maskL;}
			c1[a] = (c1[0] >> b) & maskL;
			for(int i = 0; i < a; i++)
			{c1[i] = 0l;}
		}
	}
	
	public static void chunkClear(int _length, long[] c1)
	{
		for(int i = 0; i <= _length; i++)
		{c1[i] = 0x0l;}
	}
	
	public LFloat copy()
	{
		LFloat ans = new LFloat(length);
		ans.negative = negative;
		ans.expo = expo;
		ans.state = state;
		ans.chunk = new long[length + 1];
		for(int i = 0; i < chunk.length; i++)
		{ans.chunk[i] = chunk[i];}
		return ans;
	}
	
	public LFloat copy(int _length)
	{
		LFloat ans = new LFloat(_length);
		ans.negative = negative;
		ans.expo = expo;
		ans.state = state;
		ans.chunk = new long[_length + 1];
		int min = (length < _length) ? length : _length;
		for(int i = 0; i < min + 1; i++)
		{ans.chunk[i] = chunk[i];}
		return ans;
	}
	
	public long toLong()
	{
		if(expo < 0)
		{return 0;}
		else if(expo <= 32)
		{
			long ans = (1l << expo) | (chunk[1] >>> (32 - expo));
			return negative ? -ans : ans;
		}
		else if(expo <= 62)
		{
			long ans = (1l << expo) | (chunk[1] << (expo - 32)) | (chunk[2] >>> (64 - expo));
			return negative ? -ans : ans;
		}
		else
		{return -1;}
	}
	
	public String toString()
	{
		return dec();
	}
	
	public String dec()
	{
		return dec(this);
	}
	
	public static String dec(LFloat lf)
	{
		if(lf.state == 2)
		{return "NAN";}
		else if(lf.state == 1)
		{return (lf.negative ? "-" : "+") + "INF";}
		
		LFloat a1 = lf.copy(lf.length + 1);
		int _length = a1.length;
		a1.normalize();
		LFloat exp2 = new LFloat(_length, a1.expo);
		LFloat aln2 = log(new LFloat(_length, 2)).mul(exp2);
		LFloat ln10 = log(new LFloat(_length, 10));
		long exp = aln2.div(ln10).toLong();
		a1.expo = 0l;
		
		LFloat remain = exp(aln2.sub(ln10.mul(new LFloat(_length, exp)))).mul(a1);
		chunkShl(_length, remain.chunk, remain.expo);
		
		int[] dec = new int[_length * 9 + 1];
		long[] l1 = remain.chunk;
		long[] l2 = new long[_length + 1];
		l2[0] = 10l;
		dec[0] = (int)l1[0];
		l1[0] = 0l;
		for(int i = 1; i <= _length * 9; i++)
		{
			l1 = chunkMul(_length, l1, l2);
			dec[i] = (int)l1[0];
			l1[0] = 0l;
		}
		boolean isZero = true;
		for(int i = 0; i <= lf.length * 9; i++)
		{
			if(dec[i] > 0)
			{
				isZero = false;
				break;
			}
		}
		while(dec[0] >= 10)
		{
			for(int i = lf.length * 9; i > 1; i--)
			{dec[i] = dec[i - 1];}
			dec[1] = dec[0] % 10;
			dec[0] /= 10;
			exp += 1;
		}
		if(dec[lf.length * 9] >= 5)
		{
			dec[lf.length * 9 - 1] += 1;
			for(int i = lf.length * 9 - 1; i > 0; i--)
			{
				if(dec[i] == 10)
				{
					dec[i - 1] += 1;
					dec[i] -= 10;
				}
				else
				{
					break;
				}
			}
			if(dec[0] >= 10)
			{
				for(int i = lf.length * 9; i > 1; i--)
				{dec[i] = dec[i - 1];}
				dec[1] = dec[0] % 10;
				dec[0] /= 10;
				exp += 1;
			}
		}
		while(!isZero && dec[0] == 0)
		{
			for(int i = 0; i < lf.length * 9; i++)
			{dec[i] = dec[i + 1];}
			dec[lf.length * 9] = 0;
			exp -= 1;
		}
		
		StringBuilder result = new StringBuilder();
		if(isZero)
		{
			//a1.negative = false;
			exp = 0;
		}
		result.append(a1.negative ? "-" : "");
		result.append(dec[0] + ".");
		for(int i = 1; i < lf.length * 9; i++)
		{
			result.append(dec[i]);
		}
		result.append("E" + exp);
		return result.toString();
	}
	
	public String bin()
	{
		StringBuilder result = new StringBuilder();
		long[] a1 = copy().chunk;
		chunkShl(length, a1, expo);
		result.append(negative ? "-" : "");
		int a = 0;
		for(; a < 32; a++)
		{
			if((a1[0] & (0x80000000l >>> a)) != 0)
			{break;}
		}
		for(; a < 32; a++)
		{
			result.append(((a1[0] & (0x80000000l >>> a)) != 0) ? "1" : "0");
		}
		result.append(".");
		for(int i = 0; i < 64; i++)
		{
			int m = i / 32 + 1;
			int n = i % 32;
			result.append(((a1[m] & (0x80000000l >>> n)) != 0) ? "1" : "0");
		}
		return result.toString();
	}
	
	public String hex()
	{
		StringBuilder result = new StringBuilder();
		
		result.append(negative ? "-" : "+");
		result.append(" " + expo + " ");
		String[] str = new String[length + 1];
		for(int i = 0; i <= length; i++)
		{
			str[i] = padLong(Long.toHexString(chunk[i])).substring(8, 16);
		}
		result.append(String.join(" ", str));
		return result.toString();
	}
	
	public static String padLong(String str)
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < 16 - str.length(); i++)
		{sb.append('0');}
		sb.append(str);
		return sb.toString();
	}
	
	public static LFloat log(LFloat x)
	{
		if(x.negative || x.state == 2)
		{
			LFloat ans = new LFloat(x.length);
			ans.negative = false;
			ans.state = 2;
			return ans;
		}
		else if(x.state == -1)
		{
			LFloat ans = new LFloat(x.length);
			ans.negative = true;
			ans.state = 1;
			return ans;
		}
		else if(x.state == 1)
		{
			LFloat ans = new LFloat(x.length);
			ans.negative = false;
			ans.state = 1;
			return ans;
		}
		else
		{
			LFloat _x = x.copy(x.length + 1);
			
			int _length = _x.length;
			if(Math.abs(_x.expo) > 1)
			{
				LFloat n = new LFloat(_length, _x.expo);
				LFloat ln2 = log(new LFloat(_length, 2));
				_x.expo = 0;
				return n.mul(ln2).add(log(_x)).copy(x.length);
			}
			else
			{
				LFloat ans = new LFloat(_length, 0);
				LFloat xn = _x.sub(new LFloat(_length, 1)).div(_x.add(new LFloat(_length, 1)));
				LFloat ratio2 = xn.mul(xn);
				int n = 1;
				while(xn.expo > ans.expo - 32 * _length)
				{
					ans.selfAdd(xn.div(new LFloat(_length, n - 0.5)));
					xn = xn.mul(ratio2);
					n += 1;
					if(n > 1000 * _length)
					{throw new RuntimeException("LFloat -> too many iterations!");}
				}
				ans.normalize();
				return ans.copy(x.length);
			}
		}
	}
	
	public static LFloat exp(LFloat x)
	{
		LFloat _x = x.copy(x.length + 1);
		
		int _length = _x.length;
		LFloat ln2 = log(new LFloat(_length, 2));
		long exp = _x.div(ln2).toLong();
		LFloat remain = _x.sub(ln2.mul(new LFloat(_length, exp)));
		LFloat ans = new LFloat(_length, 0);
		LFloat xn = new LFloat(_length, 1);
		int n = 1;
		while(xn.expo > ans.expo - 32 * _length)
		{
			ans.selfAdd(xn);
			xn = xn.mul(remain.div(new LFloat(_length, n)));
			n += 1;
			if(n > 1000 * _length)
			{throw new RuntimeException("LFloat -> too many iterations!");}
		}
		ans.expo += exp;
		
		ans.normalize();
		return ans.copy(x.length);
	}
}