package output;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.*;

import physical.String1D;

public class Scope1D extends JFrame
{
	public String1D father;
	
	private Scope1DCanvas canvas = new Scope1DCanvas(this);
	
	private int fps = 0;
	private long lastPaint = 0;
	private long lastUpdate = 0;
	
	
	public Scope1D(String1D _father)
	{
		super("Scope1D");
		father = _father;
		
		setLayout(null);
		
		add(canvas);
		canvas.setBounds(18, 6, 800, 600);
		setSize(850, 650);
		
		setVisible(true);
	}
	
	public void refresh()
	{
		fps += 1;
		if(System.currentTimeMillis() - lastUpdate > 500)
		{
			setTitle("Scope1D fps: " + 1000f / (System.currentTimeMillis() - lastUpdate) * fps + String.format("    time: %.5fs", father.simulationTime));
			lastUpdate = System.currentTimeMillis();
			fps = 0;
		}
		
		if(System.currentTimeMillis() - lastPaint > 20)
		{
			lastPaint = System.currentTimeMillis();
			
			canvas.myPaint();
		}
		shoot();
	}
	
	private int shootCount = 0;
	private double lastShoot = 0;
	public synchronized void shoot()
	{
		if(father.simulationTime - lastShoot < 0.03)
		{
			return;
		}
		lastShoot = father.simulationTime;
		try
		{
			File outputfile = new File("_pic/img" + shootCount + ".png");
			ImageIO.write(canvas.image, "png", outputfile);
			System.out.println("saved picture: \t" + "_pic/img" + shootCount);
			shootCount++;
		}
		catch(IOException e)
		{
			System.out.println("save picture failed!");
		}
	}
}

class Scope1DCanvas extends JPanel
{
	public Scope1D father;
	public int[] size = new int[] {800, 600};
	public double zoom = 30;
	public double centerX = 0;
	public double centerY = 0;
	
	public long lastPaint = 0;
	
	public BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
	Graphics graphics = image.getGraphics();
	
	
	public Scope1DCanvas(Scope1D _father)
	{
		father = _father;
		
		MouseHandler mh = new MouseHandler();
		addMouseListener(mh);
		addMouseMotionListener(mh);
		addMouseWheelListener(mh);
	}
	
	public synchronized void myPaint()
	{
		if(System.currentTimeMillis() - lastPaint < 5)
		{return;}
		
		graphics.setColor(new Color(255, 255, 255));
		graphics.fillRect(0, 0, size[0], size[1]);
		
		String1D obj = father.father;
		
		int[] dot = new int[2];
		dot[0] = 0;
		dot[1] = 0;
		graphics.setColor(new Color(0, 0, 0));
		for(int i = 0; i < size[0]; i++)
		{
			double[] pos = getPos(i, 0);
			double strength = obj.getStrength(pos[0]);
			
			int[] newDot = new int[2];
			newDot[0] = i;
			newDot[1] = (int)(-200 * strength + 0.5 * size[1]);
			
			graphics.drawLine​(dot[0], dot[1], newDot[0], newDot[1]);
			dot = newDot;
		}
		
		graphics.setColor(new Color(255, 255, 255));
		graphics.drawRect(0, 0, size[0] - 1, size[1] - 1);
		this.repaint();
		
		lastPaint = System.currentTimeMillis();
	}
	
	public void paint(Graphics g)
	{
		g.drawImage(image, 0, 0, null);
	}
	
	private double[] getPos(int i, int j)
	{
		double x = (i - 0.5 * size[0]) / zoom + centerX;
		double y = -(j - 0.5 * size[1]) / zoom + centerY;
		return new double[] {x, y};
	}
	
	private class MouseHandler extends MouseAdapter implements MouseWheelListener
	{
		private double oldCenterX;
		private double oldCenterY;
		Point start = new Point(0, 0);
		Point current = new Point(0, 0);
		@Override
		public void mousePressed(MouseEvent me)
		{
			oldCenterX = centerX;
			oldCenterY = centerY;
			start = me.getPoint();
		}
		@Override
		public void mouseDragged(MouseEvent me)
		{
			int dx = me.getX() - start.x;
			int dy = me.getY() - start.y;
			centerX = oldCenterX - dx / zoom;
			centerY = oldCenterY + dy / zoom;
			myPaint();
		}
		@Override
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			if(e.getWheelRotation() == 1)
			{
				zoom *= 1.1;
			}
			if(e.getWheelRotation() == -1)
			{
				zoom /= 1.1;
			}
			myPaint();
		}
	}
}