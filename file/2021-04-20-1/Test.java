import largeFloat.LFloat;

//example case using LFloat;
class Test
{
	public static void main(String[] args)
	{
		//6*4 bytes float, which has 62 bits exponent, and 6*4*8 bits significand.
		//f6 is used as a convenient generator, whose use can be seen later.
		//6 is the "length" of the LFloat, and length=2 means the number is a bit more precise than double.
		
		/*
		common operations:
			add
			sub
			mul
			div
			selfAdd
			selfSub
		
			copy(): return a copy of self
			copy(int length): return a copy of self, but with specific "length" (transform)
		
			log
			exp
		*/
		
		LFloat f6 = new LFloat(6);
		LFloat[] num = new LFloat[100];
		
		//trivial constructor;
		num[0] = new LFloat(6, "1.0");
		System.out.println("num[0] = " + num[0]);
		
		//convenient generator, which generates a LFloat with the same length;
		num[1] = f6.num("2.0");
		System.out.println("num[1] = " + num[1]);
		
		//operations must happen between numbers of the same "length"
		num[2] = num[1].add(num[0]);
		System.out.println("num[2] = " + num[2]);
		
		num[2].selfAdd(num[0]);
		System.out.println("num[2] = " + num[2]);
		
		num[3] = num[2].mul(num[2]);
		System.out.println("num[3] = " + num[3]);
		
		num[4] = num[2].div(num[3]);
		System.out.println("num[4] = " + num[4]);
		
		
		//extremity test:
		
		num[5] = f6.num("1e1000000000000000");
		System.out.println("num[5] = " + num[5]);
		
		num[6] = f6.num("-0.2e-1000000000000000");
		System.out.println("num[6] = " + num[6]);
		
		
		//log and exp accuracy test:
		
		num[7] = LFloat.exp(LFloat.log(f6.num(10)).mul(f6.num("100000000000000")));
		System.out.println("num[7] = " + num[7]);
		
		//fun game of mathematics:
		LFloat pi = calcPi(50);
		System.out.println("pi = " + pi + " :-)");
	}
	
	//calculate pi using "Newton / Euler Convergence Transformation", from https://en.wikipedia.org/wiki/Approximations_of_π
	public static LFloat calcPi(int length)
	{
		//increase calculation accuracy
		int _length = length + 1;
		LFloat lf = new LFloat(_length);
		
		LFloat ans = lf.num(0);
		LFloat xn = lf.num(2);
		int n = 1;
		while(xn.expo > ans.expo - 32 * _length)
		{
			ans.selfAdd(xn);
			xn = xn.mul(lf.num(n).div(lf.num(2 * n + 1)));
			n += 1;
		}
		
		ans.normalize();
		//return accuracy to normal
		return ans.copy(length);
	}
}