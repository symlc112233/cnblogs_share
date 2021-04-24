import physical.String1D;

class Test extends Thread
{
	String1D string = new String1D();
	
	public static void main(String[] args)
	{
		Test test = new Test();
		test.start();
	}
	
	public double lastFrame;
	public double tMin = 0.01;
	public void run()
	{
		lastFrame = System.currentTimeMillis();
		
		while(1 == 1)
		{
			mainLoop();
		}
	}
	
	public void mainLoop()
	{
		while(System.currentTimeMillis() < lastFrame + tMin)
		{
			try
			{Thread.sleep(1);}
			catch(InterruptedException e)
			{e.printStackTrace();}
		}
		if(System.currentTimeMillis() > lastFrame + 10 * tMin)
		{lastFrame = System.currentTimeMillis() - 9 * tMin;}
		lastFrame += tMin;
		
		string.step();
	}
}