package physical;

public class TipNode
{
	public double lastInput = 0;
	public double pos = 0;
	public double base = 0;
	double ratio;
	int maxN = 2000;
	double[] reactions = new double[maxN];
	double[] inputs = new double[maxN];
	
	
	public TipNode(double _ratio)
	{
		ratio = _ratio;
		for(int i = 0; i < maxN; i++)
		{
			reactions[i] = reaction(ratio * (i + 1));
		}
	}
	
	public void refresh(double input)
	{
		//pos = input;
		if(false)
		return;
		
		//pos += 0.000001;
		base += inputs[maxN - 1];
		for(int i = maxN - 1; i > 0; i--)
		{
			inputs[i] = inputs[i - 1];
		}
		inputs[0] = input - lastInput;
		lastInput = input;
		double sum = 0;
		for(int i = 0; i < maxN; i++)
		{
			sum += inputs[i] * reactions[i];
		}
		//System.out.println(sum);
		pos = sum + base;
	}
	
	
	public static double[] taylors = new double[80];
	static
	{
		taylors[1] = 0.25;
		for(int i = 2; i < taylors.length; i++)
		{
			taylors[i] = taylors[i - 1] * (-0.5 / (i * (i + 1)));
		}
	}
	public static double reaction(double x)
	{
		if(x > 30)
		{
			return 1;
		}
		double sum = 0;
		double mul1 = 1;
		for(int i = 0; i < taylors.length; i++)
		{
			sum += taylors[i] * mul1;
			mul1 *= x * x;
		}
		return sum;
	}
	
	public static void main(String[] args)
	{
		for(int i = 0; i < 5; i++)
		{
			System.out.println(taylors[i]);
		}
		System.out.println(reaction(30));
	}
}