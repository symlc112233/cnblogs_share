import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.io.File;

public class CreateGIF
{

	public static void main(String[] args) throws Exception
	{
		BufferedImage first = ImageIO.read(new File("0.png"));
		ImageOutputStream output = new FileImageOutputStream(new File("gif/" + System.currentTimeMillis() + ".gif"));
		
		GifSequenceWriter writer = new GifSequenceWriter(output, first.getType(), 30, true);
		//writer.writeToSequence(first);
		
		File[] images = new File[1000];
		for(int i = 0; i < 1000; i++)
		{
			File file = findFile("img" + i + ".png");
			images[i] = file;
		};
		
		for(File image : images)
		{
			if(image != null)
			{
				System.out.println("->\t" + image.getPath());
				BufferedImage next = ImageIO.read(image);
				writer.writeToSequence(next);
			}
		}
		
		writer.close();
		output.close();
		System.out.println("created gif:\tgif/" + System.currentTimeMillis() + ".gif");
	}
	
	public static File findFile(String fileName)
	{
		File tempFile = new File(fileName);
		if(tempFile.exists())
		{
			return tempFile;
		}
		else
		{
			return null;
		}
		
	}
}

class GifSequenceWriter
{

	protected ImageWriter writer;
	protected ImageWriteParam params;
	protected IIOMetadata metadata;
	
	public GifSequenceWriter(ImageOutputStream out, int imageType, int delay, boolean loop) throws IOException
	{
		writer = ImageIO.getImageWritersBySuffix("gif").next();
		params = writer.getDefaultWriteParam();
		
		ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
		metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params);
		
		configureRootMetadata(delay, loop);
		
		writer.setOutput(out);
		writer.prepareWriteSequence(null);
	}
	
	private void configureRootMetadata(int delay, boolean loop) throws IIOInvalidTreeException
	{
		String metaFormatName = metadata.getNativeMetadataFormatName();
		IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);
		
		IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
		graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
		graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
		graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
		graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(delay / 10));
		graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");
		
		IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
		commentsNode.setAttribute("CommentExtension", "Created by: https://memorynotfound.com");
		
		IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
		IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
		child.setAttribute("applicationID", "NETSCAPE");
		child.setAttribute("authenticationCode", "2.0");
		
		int loopContinuously = loop ? 0 : 1;
		child.setUserObject(new byte[] { 0x1, (byte)(loopContinuously & 0xFF), (byte)((loopContinuously >> 8) & 0xFF)});
		appExtensionsNode.appendChild(child);
		metadata.setFromTree(metaFormatName, root);
	}
	
	private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName)
	{
		int nNodes = rootNode.getLength();
		for(int i = 0; i < nNodes; i++)
		{
			if(rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName))
			{
				return (IIOMetadataNode) rootNode.item(i);
			}
		}
		IIOMetadataNode node = new IIOMetadataNode(nodeName);
		rootNode.appendChild(node);
		return(node);
	}
	
	public void writeToSequence(RenderedImage img) throws IOException
	{
		writer.writeToSequence(new IIOImage(img, null, metadata), params);
	}
	
	public void close() throws IOException
	{
		writer.endWriteSequence();
	}
	
}