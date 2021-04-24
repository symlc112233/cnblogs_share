package physical;

import physical.TipNode;
import output.Scope1D;
import java.io.*;


public class String1D
{
	public double soundSpd = 10;
	
	public double range = 10;
	public double dx = 0.02;
	
	public double ratio = 0.05;
	public double dt = ratio * dx / soundSpd;
	
	public int N = (int)(2d * range / dx + 1);
	public double[] X = new double[N];
	
	public double[] acl = new double[N];
	public double[] spd = new double[N];
	public double[] pos = new double[N];
	
	public Scope1D scope = new Scope1D(this);
	public TipNode tipNode = new TipNode(ratio);
	
	public double simulationTime;
	
	public String1D()
	{
		for(int i = 0; i < N; i++)
		{
			X[i] = (i - 0.5 * N) * dx;
		}
	}
	
	public void step()
	{
		checkIterate();
		checkRefresh();
		simulationTime += dt;
		
		scope.refresh();
	}
	
	private double phi = 0;
	private void checkRefresh()
	{
		if(phi < 2 * Math.PI)
		{
			phi += 2.0 * Math.PI * dt;
		}
		
		pos[0] = 0.2 * Math.sin(phi);
	}
	
	private double[] k1 = new double[N];
	private void checkIterate()
	{
		double mul1 = 0.5d / (dx * dx);
		for(int i = 1; i < N - 1; i++)
		{
			k1[i] = mul1 * (pos[i - 1] + pos[i + 1] - 2 * pos[i]);
		}
		k1[N - 1] = mul1 * (pos[N - 2] + tipNode.pos - 2 * pos[N - 1]);
		
		tipNode.refresh(pos[N - 1]);
		
		double mul2 = soundSpd * soundSpd * dt;
		for(int i = 0; i < N; i++)
		{
			spd[i] += mul2 * k1[i];
			pos[i] += dt * spd[i];
		}
	}
	
	private void vecAdd(double[] vec1, double[] vec2, double[] ktemp)
	{
		int b = vec1.length;
		for(int j = 0; j < b; j++)
		{
			ktemp[j] = vec1[j] + vec2[j];
		}
	}
	
	public double getStrength(double _x)
	{
		double mul1 = 1.0 / dx;
		int i = (int)(mul1 * _x + 0.5 * N);
		if(i < 0 || i >= N)
		{
			return Double.NaN;
		}
		else
		{
			double mag = pos[i];
			return mag;
		}
	}
}