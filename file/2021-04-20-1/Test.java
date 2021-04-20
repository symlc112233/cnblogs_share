import java.io.*;

class Test
{
	static int length = 200;
	static LFloat f8 = new LFloat(2);
	static String[] series = new String[length];
	static LFloat[] taylor = new LFloat[10000];
	
	static int total = 1000;
	static LFloat[] xs = new LFloat[10000];
	static LFloat[] ys = new LFloat[10000];
	
	public static void main(String[] args)
	{
		generateTaylor();
		generateX();
		generateY();
		//print();
		doSave();
	}
	
	public static void generateTaylor()
	{
		taylor[0] = f8.num(2);
		for(int i = 1; i < length; i++)
		{
			LFloat offset = f8.num(-0.4);
			
			LFloat mul = f8.num(-2).mul(offset.add(f8.num(i))).mul(offset.add(f8.num(i + 1)));
			taylor[i] = taylor[i - 1].div(mul);
			//System.out.println(taylor[i]);
		}
		for(int i = 0; i < length; i++)
		{
			series[i] = taylor[i].toString();
		}
	}
	
	public static void generateX()
	{
		for(int i = 0; i < total; i++)
		{
			xs[i] = f8.num(0.1 * i);
		}
	}
	
	public static void generateY()
	{
		for(int i = 0; i < total; i++)
		{
			LFloat x = xs[i].mul(xs[i]);
			LFloat xn = f8.num(1);
			LFloat sum = f8.num(0);
			
			for(int j = 0; j < length; j++)
			{
				sum.selfAdd(xn.mul(taylor[j]));
				xn = xn.mul(x);
			}
			ys[i] = sum;
		}
	}
	
	public static void print()
	{
		for(int i = 0; i < total; i++)
		{
			System.out.println(ys[i]);
		}
	}
	
	public static void doSave()
	{
		try
		{
			StringBuffer result = new StringBuffer();
			for(int i = 0; i < total; i++)
			{
				result.append(xs[i] + ", ");
			}
			result.append("\n");
			for(int i = 0; i < total; i++)
			{
				result.append(ys[i] + ", ");
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter("./data0.txt"));
			bw.write(result.toString());
			bw.close();
		}
		catch(Exception e)
		{}
	}
}