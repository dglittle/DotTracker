

// add a button to make the video disappear
// add a title


// ---------done---------------------------------------
// make a button that pops up the angle tracker window again
// make the angle appear on the green dot
// make it show a dark line for 0, 90, and 180
// make it not clear it's old path
// make it so there isn't that wierd line when a new tracker starts
// make it so when you add the first tracker, the scroll-bar appears
// make it so you can right click and remove connectors
// make it so it chooses the color under the center after it does the first update
// make it so you can right-click on a tracker to delete it
// make advancing a frame call mouseDragged, but only if newConnection is not null
// make it so that you don't necessarily draw a line if you click, the target moves away, and you release; make it so if you don't actually move the mouse, then you won't end up drawing a line.









import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.regex.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.nio.channels.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.jar.*;

class Vector2D {
	double x, y;
	
	public Vector2D(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public Vector2D(Point that) {
		this.x = that.x;
		this.y = that.y;
	}
	public Vector2D add(Vector2D that) {
		return new Vector2D(x + that.x, y + that.y);
	}
	public Vector2D sub(Vector2D that) {
		return new Vector2D(x - that.x, y - that.y);
	}
	public double dot(Vector2D that) {
		return x * that.x + y * that.y;
	}
	public double dist(Vector2D that) {
		return this.sub(that).length();
	}
	public double length() {
		return (float)Math.sqrt(this.dot(this));
	}
	public Vector2D normal() {
		double length = length();
		return new Vector2D(x / length(), y / length());
	}
	public Vector2D perpendicular() {
		return new Vector2D(y, -x);
	}
	public Vector2D mul(double s) {
		return new Vector2D(x * s, y * s);
	}
	public Point getPoint() {
		return new Point((int)x, (int)y);
	}
	public static Vector2D midPoint(Vector2D a, Vector2D b) {
		return a.add(b).mul(0.5);
	}
}

class MyUtil {
	public static Vector2D[] getCircleCircleIntersects(Vector2D a, double ra, Vector2D b, double rb) {
		Vector2D AtoB = b.sub(a);
		double d = AtoB.length();
		if (ra + rb <= d) {
			Vector2D v = Vector2D.midPoint(a.add(AtoB.normal().mul(ra)), b.add(AtoB.normal().mul(-rb)));
			return new Vector2D[] {v, v};
		} else if (d + rb < ra) {
			Vector2D v = Vector2D.midPoint(a.add(AtoB.normal().mul(ra)), b.add(AtoB.normal().mul(rb)));
			return new Vector2D[] {v, v};
		} else if (d + ra < rb) {
			Vector2D v = Vector2D.midPoint(a.add(AtoB.normal().mul(-ra)), b.add(AtoB.normal().mul(-rb)));
			return new Vector2D[] {v, v};
		} else {
			double x = (Math.pow(d, 2) - Math.pow(rb, 2) + Math.pow(ra, 2)) / (2.0 * d);
			double y = Math.sqrt(Math.pow(ra, 2) - Math.pow(x, 2));
			Vector2D midPoint = a.add(AtoB.normal().mul(x));
			Vector2D midPointToIntersect = AtoB.normal().perpendicular().mul(y);
			return new Vector2D[] {
				midPoint.add(midPointToIntersect),
				midPoint.sub(midPointToIntersect),
			};
		}
	}
	public static double distFromPointToLine(int px, int py, int x1, int y1, int x2, int y2) {
		Vector2D p = new Vector2D(px, py);
		Vector2D e1 = new Vector2D(x1, y1);
		Vector2D e2 = new Vector2D(x2, y2);
		
		if (e2.sub(e1).dot(p.sub(e1)) < 0) {
			// closest to e1
			return e1.dist(p);
		} else if (e1.sub(e2).dot(p.sub(e2)) < 0) {
			// closest to e2
			return e2.dist(p);
		} else {
			Vector2D unitDir = e2.sub(e1).normal();
			Vector2D toPoint = p.sub(e1);
			return toPoint.sub(unitDir.mul(toPoint.dot(unitDir))).length();			
		}
	}
	public static double lerp(double t1, double x1, double t2, double x2, double t) {
		return x1 + (x2 - x1) * ((t - t1) / (t2 - t1));
	}
    public static void drawString(Graphics g, String text, int x, int y, double hotX, double hotY) {    
        FontMetrics f = g.getFontMetrics();
        Rectangle2D r = f.getStringBounds(text, g);
        g.drawString(text,
            x - (int)(hotX * r.getWidth()),
            y + (int)(hotY * r.getHeight()) - (int)(r.getHeight() + r.getY()));
    }
	public static void sortArrayUsingArray(Object[] sortMe, double[] usingMe, boolean ascending) {
		class Pair implements Comparable {
			public Object a;
			public double b;
			public boolean ascending;
			public Pair(Object a, double b, boolean ascending) {
				this.a = a;
				this.b = b;
				this.ascending = ascending;
			}
			public int compareTo(Object o) {
				Pair that = (Pair)o;
				return (new Double(this.b).compareTo(new Double(that.b))) * (ascending ? 1 : -1);
			}
		}
		Vector v = new Vector();
		for (int i = 0; i < sortMe.length; i++) {
			v.add(new Pair(sortMe[i], usingMe[i], ascending));
		}
		Collections.sort(v);
		for (int i = 0; i < sortMe.length; i++) {
			sortMe[i] = ((Pair)v.get(i)).a;
		}
	}
}

public class DotTracker2 extends JFrame {
	public static void main(String[] args) throws Exception {
		new DotTracker2();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	
	MyTimer timer = new MyTimer();
	MyCanvas canvas;
	PlayStopButton playStopButton;
	int frameIndex = 0;
	Vector frameFiles = new Vector();
	BufferedImage frameImage;
	Vector trackers = new Vector();
	Vector connectors = new Vector();
	Vector angleTrackers = new Vector();
	AffineTransform transform = new AffineTransform();
	JFrame angleTrackerDialog;
	AngleTrackerCanvas angleTrackerCanvas;
	boolean backgroundVisible = true;
	
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public DotTracker2() throws Exception {
		super("MotionEase - 2D Demo");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(canvas = new MyCanvas());
		getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);
		getContentPane().setBackground(Color.BLACK);
		loadCurrentFrame();
		
		JFrame f = new JFrame("Angle Tracker");
		angleTrackerDialog = f;
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(new JScrollPane(angleTrackerCanvas = new AngleTrackerCanvas()));
		f.setSize(600, 300);
		
		setSize(800, 600);
		show();
	}
	public JPanel createButtonPanel() throws Exception {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.BLACK);
		p.setBorder(new EmptyBorder(20, 20, 20, 20));
		
		Box b = Box.createHorizontalBox();		
		b.add(new MyButton("New", "Load a new animation", new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				onNew();
			}
		}));
		b.add(Box.createHorizontalStrut(32));
		b.add(new MyButton("Rewind", "Rewind", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onRewind();
			}
		}));
		b.add(Box.createHorizontalStrut(4));
		b.add(playStopButton = new PlayStopButton(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onPlayStop();
			}
		}));
		b.add(Box.createHorizontalStrut(4));
		b.add(new MyButton("Step", "Step forward 1 frame", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onStepForward();
			}
		}));
		p.add(b, BorderLayout.WEST);
		
		b = Box.createHorizontalBox();		
		b.add(new MyButton("Angle", "Show \"Angle Tracker\" dialog", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAngle();
			}
		}));
		b.add(Box.createHorizontalStrut(32));
		b.add(new MyButton("BackgroundToggle", "Toggle video background", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onBackgroundToggle();
			}
		}));
		b.add(Box.createHorizontalStrut(32));
		b.add(new MyButton("Clear", "Clear all the trackers", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onClear();
			}
		}));
		p.add(b, BorderLayout.EAST);
		
		p.add(new TitlePanel(getTitle()));
		
		return p;
	}
	class MyButton extends JButton {
		public MyButton(ActionListener a) throws Exception {
			setBackground(Color.BLACK);
			setBorder(new EmptyBorder(0, 0, 0, 0));
			addActionListener(a);
		}
		public MyButton(String prefix, String tooltip, ActionListener a) throws Exception {
			this(a);
			setIcons(getIcons(prefix));
			setToolTipText(tooltip);
		}
		public Vector getIcons(String prefix) throws Exception {
			Vector v = new Vector();
			v.add(new ImageIcon(ImageIO.read(new File(prefix + "Normal.png"))));
			v.add(new ImageIcon(ImageIO.read(new File(prefix + "Selected.png"))));
			v.add(new ImageIcon(ImageIO.read(new File(prefix + "Pressed.png"))));
			return v;
		}
		public void setIcons(Vector icons) {		
			ImageIcon normal = (ImageIcon)icons.get(0);
			ImageIcon selected = (ImageIcon)icons.get(1);
			ImageIcon pressed = (ImageIcon)icons.get(2);
			setDisabledIcon(normal);
			setDisabledSelectedIcon(normal);
			setIcon(normal);
			setPressedIcon(pressed);
			setRolloverEnabled(true);
			setRolloverIcon(selected);
			setRolloverSelectedIcon(selected);
			setSelectedIcon(selected);
		}
	}
	class PlayStopButton extends MyButton {
		Vector playIcons;
		Vector stopIcons;
		
		public PlayStopButton(ActionListener a) throws Exception {
			super(a);
			playIcons = getIcons("Play");
			stopIcons = getIcons("Stop");
			setPlayState(false);
		}
		public void setPlayState(boolean play) {
			if (play) {
				setIcons(stopIcons);
				setToolTipText("Stop");
			} else {
				setIcons(playIcons);
				setToolTipText("Play");
			}
		}
	}
	class MyTimer extends javax.swing.Timer implements ActionListener {
		public MyTimer() {
			super(30, null);
			addActionListener(this);
		}
		public void actionPerformed(ActionEvent e) {
			onTimer();
		}
		public void start() {
			super.start();
			updatePlayStop();
		}
		public void restart() {
			super.restart();
			updatePlayStop();
		}
		public void stop() {
			super.stop();
			updatePlayStop();
		}
	}
	class TitlePanel extends JPanel {
		public String title;
			
		public TitlePanel(String title) {
			this.title = title;
			setBackground(Color.BLACK);
		}
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Color.GREEN);
			double scale = 1.5;
			g.setFont(g.getFont().deriveFont(new AffineTransform(scale, 0, 0, scale, 0, 0)));
			MyUtil.drawString(g, title, getWidth() / 2, getHeight() / 2, 0.5, 0.5);
		}
	}
	class MyCanvas extends JPanel {
		boolean legalLeftMousePress = false;
		boolean legalRightMousePress = false;
		
		public MyCanvas() {
			setBackground(Color.BLACK);
			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					Point p = new Point(e.getX(), e.getY());
					try {
						transform.inverseTransform(p, p);
					} catch (Exception ee) {}
					
					if (e.getButton() == MouseEvent.BUTTON1) {
						if (p.x >= 0 && p.y >= 0 && p.x < frameImage.getWidth() && p.y < frameImage.getHeight()) {
							legalLeftMousePress = true;
							onMousePressed(p.x, p.y);
						}
					}
					if (e.getButton() == MouseEvent.BUTTON3) {
						if (p.x >= 0 && p.y >= 0 && p.x < frameImage.getWidth() && p.y < frameImage.getHeight()) {
							legalRightMousePress = true;
							onRightMousePressed(p.x, p.y);
						}
					}
				}
				public void mouseReleased(MouseEvent e) {
					Point p = new Point(e.getX(), e.getY());
					try {
						transform.inverseTransform(p, p);
					} catch (Exception ee) {}
						
					if (e.getButton() == MouseEvent.BUTTON1 && legalLeftMousePress) {
						if (p.x >= 0 && p.y >= 0 && p.x < frameImage.getWidth() && p.y < frameImage.getHeight())
							onMouseReleased(p.x, p.y);
						else
							onMouseReleased(-1, -1);
						legalLeftMousePress = false;
					}
					if (e.getButton() == MouseEvent.BUTTON3 && legalRightMousePress) {
						if (p.x >= 0 && p.y >= 0 && p.x < frameImage.getWidth() && p.y < frameImage.getHeight())
							onRightMouseReleased(p.x, p.y);
						else
							onRightMouseReleased(-1, -1);
						legalRightMousePress = false;
					}
				}
			});
			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent e) {
					Point p = new Point(e.getX(), e.getY());
					try {
						transform.inverseTransform(p, p);
					} catch (Exception ee) {}
						
					if (legalLeftMousePress) {
						if (p.x >= 0 && p.y >= 0 && p.x < frameImage.getWidth() && p.y < frameImage.getHeight())
							onMouseDragged(p.x, p.y);
					}
					if (legalRightMousePress) {
						if (p.x >= 0 && p.y >= 0 && p.x < frameImage.getWidth() && p.y < frameImage.getHeight())
							onRightMouseDragged(p.x, p.y);
					}
				}
			});
			addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					updateTransform();					
				}
			});
		}
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			onPaint((Graphics2D)g);
		}
		public void updateTransform() {
			transform = new AffineTransform();
			transform.translate((getWidth() - frameImage.getWidth()) / 2, (getHeight() - frameImage.getHeight()) / 2);
		}
	}
	class AngleTrackerCanvas extends JPanel {
		public AngleTrackerCanvas() {
			setBackground(Color.BLACK);
		}
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			onPaintAngleTrackerCanvas((Graphics2D)g);
		}
	}
	public void repaint() {
		super.repaint();
		angleTrackerCanvas.repaint();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	
	String onNew_beginsWith;
	public void onNew() {
		FileDialog f = new FileDialog(this, "Select File from Animation to Load");
		f.show();
		if (f.getFile() != null) {
			Matcher m = Pattern.compile("(.*?)\\d+\\.").matcher(f.getFile());			
			if (m.find()) {
				onNew_beginsWith = m.group(1);
				File[] files = (new File(f.getDirectory())).listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith(onNew_beginsWith);
					}
				});
				frameFiles = new Vector(Arrays.asList(files));
				Collections.sort(frameFiles, new Comparator() {
					public int compare(Object o1, Object o2) {
						String s1 = (String)((File)o1).getName();
						String s2 = (String)((File)o2).getName();
						Matcher m1 = Pattern.compile(".*?(\\d+)\\.").matcher(s1);
						Matcher m2 = Pattern.compile(".*?(\\d+)\\.").matcher(s2);
						if (m1.find() && m2.find()) {
							return (new Integer(m1.group(1))).compareTo(new Integer(m2.group(1)));
						}
						throw new IllegalArgumentException("Illegal filename: " + s1 + ", or " + s2);
					}
				});
				timer.stop();
				frameIndex = 0;
				loadCurrentFrame();
				clearTrackers();
				repaint();
			} else {
				JOptionPane.showMessageDialog(this, "File not part of an animation: " + f.getFile());
			}
		}
	}
	public void onRewind() {
		timer.stop();
		frameIndex = 0;
		loadCurrentFrame();
		resetTrackers();
		repaint();
	}
	public void onPlayStop() {
		if (timer.isRunning()) {
			timer.stop();
		} else {
			timer.restart();
		}
	}
	public void onStepForward() {
		timer.stop();
		onTimer();
	}
	public void onAngle() {
		angleTrackerDialog.show();
		angleTrackerDialog.requestFocus();
	}
	public void onBackgroundToggle() {
		backgroundVisible = !backgroundVisible;
		repaint();
	}
	public void onClear() {
		clearTrackers();
		repaint();
	}
	public void onTimer() {
		if (frameIndex < frameFiles.size() - 1) {
			frameIndex++;
			loadCurrentFrame();
			updateTrackers();
			repaint();
		} else {
			timer.stop();
		}
	}
	public void onPaint(Graphics2D g) {
		AffineTransform oldT = g.getTransform();
		g.transform(transform);
		if (backgroundVisible)
			g.drawImage(frameImage, 0, 0, this);
		g.setTransform(oldT);
		drawTrackers(g);
	}
	public void onPaintAngleTrackerCanvas(Graphics2D g) {
		drawAngleTrackerInfo(g);
	}
	
	Tracker selectedTracker = null;
	public void onMousePressed(int x, int y) {
		selectedTracker = getTracker(x, y);
		if (selectedTracker == null) {
			trackers.add(selectedTracker = new Tracker(x, y));
		}
		repaint();
	}
	public void onMouseReleased(int x, int y) {
	}
	public void onMouseDragged(int x, int y) {
	}
	
	public void onRightMousePressed(int x, int y) {
		Tracker t = getTracker(x, y);
		if (t != null) {
			removeTracker(t);
			repaint();
		} else {
			Tracker[] ts = getClosestTrackers(x, y);
			if (ts.length >= 2) {
				ts[0].jointRadius = new Vector2D(x, y).dist(new Vector2D(ts[0].center));
				ts[1].jointRadius = new Vector2D(x, y).dist(new Vector2D(ts[1].center));
				new AngleTracker(ts[0], ts[1], x, y);
				repaint();
			}
		}
	}
	public void onRightMouseReleased(int x, int y) {
	}
	public void onRightMouseDragged(int x, int y) {
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	
	public void updatePlayStop() {
		playStopButton.setPlayState(timer.isRunning());
	}
	public void loadCurrentFrame() {
		BufferedImage oldImage = frameImage;		
		frameImage = null;
		if (frameIndex < frameFiles.size()) {
			try {
				frameImage = ImageIO.read((File)frameFiles.get(frameIndex));
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "failed to load frame of animation");
			}
		}
		if (frameImage == null) {
			clearTrackers();
			frameImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		}
		if (oldImage == null || oldImage.getWidth() != frameImage.getWidth() || oldImage.getHeight() != frameImage.getHeight()) {
			canvas.updateTransform();
		}
	}
	public void clearTrackers() {
		trackers.clear();
		connectors.clear();
		angleTrackers.clear();
	}
	public void resetTrackers() {
		for (int i = 0; i < trackers.size(); i++) {
			Tracker t = (Tracker)trackers.get(i);
			t.reset();
		}
		updateAngleTrackers();
	}
	public void removeTracker(Tracker trackerToRemove) {
		for (Iterator i = trackers.iterator(); i.hasNext(); ) {
			Tracker t = (Tracker)i.next();
			
			if (t == trackerToRemove) {
				i.remove();
			}
		}
		for (Iterator i = angleTrackers.iterator(); i.hasNext(); ) {
			AngleTracker a = (AngleTracker)i.next();
			
			if (a.usesTracker(trackerToRemove)) {
				i.remove();
			}
		}
	}
	public void updateTrackers() {
		for (int i = 0; i < trackers.size(); i++) {
			Tracker t = (Tracker)trackers.get(i);
			t.update(frameImage);
		}
		updateAngleTrackers();
	}
	public void updateAngleTrackers() {
		for (int i = 0; i < angleTrackers.size(); i++) {
			AngleTracker a = (AngleTracker)angleTrackers.get(i);
			a.update();
		}
	}
	public void drawTrackers(Graphics2D g) {
		for (int i = 0; i < angleTrackers.size(); i++) {
			AngleTracker a = (AngleTracker)angleTrackers.get(i);
			a.draw1(g);
		}
		for (int i = 0; i < trackers.size(); i++) {
			Tracker t = (Tracker)trackers.get(i);
			t.draw1(g);
		}
		
		for (int i = 0; i < trackers.size(); i++) {
			Tracker t = (Tracker)trackers.get(i);
			t.draw2(g);
		}
		for (int i = 0; i < angleTrackers.size(); i++) {
			AngleTracker a = (AngleTracker)angleTrackers.get(i);
			a.draw2(g);
		}
	}
	public void drawAngleTrackerInfo(Graphics2D g) {
		int y = 0;
		for (int i = 0; i < angleTrackers.size(); i++) {
			AngleTracker a = (AngleTracker)angleTrackers.get(i);
			g.drawImage(a.image, 0, y, angleTrackerCanvas);
			y += a.image.getHeight();
		}
	}
	public void updateAngleTrackerPreferredSize() {
		int width = 0;
		int height = 0;
		for (int i = 0; i < angleTrackers.size(); i++) {
			AngleTracker a = (AngleTracker)angleTrackers.get(i);
			width = Math.max(width, a.image.getWidth());
			height += a.image.getHeight();
		}
		
		angleTrackerCanvas.setPreferredSize(new Dimension(width, height));
		angleTrackerCanvas.getParent().invalidate();
		angleTrackerCanvas.getParent().validate();
	}
	public Tracker getTracker(int x, int y) {
		Tracker closest = null;
		float closestDist = Float.MAX_VALUE;
		for (int i = 0; i < trackers.size(); i++) {
			Tracker t = (Tracker)trackers.get(i);
			
			float dist = (float)t.center.distance(x, y);
			if (dist < closestDist) {
				closestDist = dist;
				closest = t;
			}
		}
		if (closestDist < 8.0f)
			return closest;
		else
			return null;
	}
	public Tracker[] getClosestTrackers(int x, int y) {
		Tracker[] ts = new Tracker[trackers.size()];
		double[] dists = new double[ts.length];
		for (int i = 0; i < ts.length; i++) {
			ts[i] = (Tracker)trackers.get(i);
			dists[i] = ts[i].center.distance(x, y);
		}
		MyUtil.sortArrayUsingArray(ts, dists, true);
		return ts;
	}
	Vector angleColors = null;
	int angleColorsIndex = 0;
	public Color getAngleTrackerColor() {
		if (angleColors == null) {
			angleColors = new Vector();
			angleColors.add(new Color(255, 0, 0));
			angleColors.add(new Color(255, 0, 255));
			angleColors.add(new Color(255, 255, 0));
			angleColors.add(new Color(0, 255, 255));
			angleColors.add(new Color(255, 128, 0));
			angleColors.add(new Color(0, 128, 255));
		}
		Color c = (Color)angleColors.get(angleColorsIndex);
		angleColorsIndex = (angleColorsIndex + 1) % angleColors.size();
		return c;	
		/*
		while (true) {
			Color c = new Color((float)Math.random(), (float)Math.random(), (float)Math.random());
			if (getValue(c) > 230 && getDiff(c, Color.GREEN) > 100 && getDiff(c, Color.BLUE) > 50 && !colorUsed(c)) {
				return c;
			}
		}
		*/		
	}
	public boolean colorUsed(Color c) {
		for (int i = 0; i < angleTrackers.size(); i++) {
			AngleTracker a = (AngleTracker)angleTrackers.get(i);
			if (a.color.equals(c))
				return true;
		}
		return false;
	}
	public float getValue(Color c) {
		return Math.max(Math.max(c.getRed(), c.getGreen()), c.getBlue());
	}
	public float getDiff(Color a, Color b) {
		return Math.abs(a.getRed() - b.getRed()) + Math.abs(a.getGreen() - b.getGreen()) + Math.abs(a.getBlue() - b.getBlue());
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	
	class AngleTracker {
		public Tracker a;
		public Tracker b;
		public Point center;
		public int centerIndex;
		public Color color;
		public Color darkColor;
		public BufferedImage image;
		public double startAngle;
		public double angle = Double.NaN;
		public int graphX1, graphY1, graphX2, graphY2;
		public int graphWidth, graphHeight;
		
		public AngleTracker(Tracker a, Tracker b, int x, int y) {
			this.a = a;
			this.b = b;
			
			Vector2D A = new Vector2D(a.center.x, a.center.y);
			Vector2D B = new Vector2D(b.center.x, b.center.y);
			Vector2D C = new Vector2D(x, y);
			Vector2D[] cs = MyUtil.getCircleCircleIntersects(A, a.jointRadius, B, b.jointRadius);
			if (cs[0].dist(C) < cs[1].dist(C)) {
				centerIndex = 0;
			} else {
				centerIndex = 1;
			}
			center = cs[centerIndex].getPoint();
			
			color = getAngleTrackerColor();
			angleTrackers.add(this);
			darkColor = new Color(color.getRed() / 2, color.getGreen() / 2, color.getBlue() / 2);
			int horizontalBuf = 30;
			int verticalBuf = 40;
			int width = frameFiles.size() + horizontalBuf;
			int height = 100 + verticalBuf;
			graphX1 = horizontalBuf;
			graphY1 = verticalBuf / 2;
			graphX2 = width - 1;
			graphY2 = height - 1 - (verticalBuf / 2);			
			graphWidth = graphX2 - graphX1 + 1;
			graphHeight = graphY2 - graphY1 + 1;
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setColor(darkColor);
			g.drawLine(graphX1, graphY1, graphX2, graphY1);
			g.drawLine(graphX1, (int)MyUtil.lerp(0, graphY1, 1, graphY2, .5), graphX2, (int)MyUtil.lerp(0, graphY1, 1, graphY2, .5));
			g.drawLine(graphX1, graphY2, graphX2, graphY2);
			g.setColor(color);
			MyUtil.drawString(g, "0", graphX1 - 5, graphY1, 1.0, 0.5);
			MyUtil.drawString(g, "90", graphX1 - 5, (int)MyUtil.lerp(0, graphY1, 1, graphY2, .5), 1.0, 0.5);
			MyUtil.drawString(g, "180", graphX1 - 5, graphY2, 1.0, 0.5);
			
			updateAngleTrackerPreferredSize();
			update();
		}
		public boolean usesTracker(Tracker t) {
			return t == a || t == b;
		}
		public void update() {
			double oldAngle = angle;
			
			Vector2D A = new Vector2D(a.center.x, a.center.y);
			Vector2D B = new Vector2D(b.center.x, b.center.y);
			center = MyUtil.getCircleCircleIntersects(A, a.jointRadius, B, b.jointRadius)[centerIndex].getPoint();
			Vector2D C = new Vector2D(center.x, center.y);
			
			Vector2D AsubC = A.sub(C);			
			double A_angle = Math.toDegrees(Math.atan2(-AsubC.y, AsubC.x));
			if (A_angle < 0)
				A_angle += 360;
			Vector2D BsubC = B.sub(C);
			double B_angle = Math.toDegrees(Math.atan2(-BsubC.y, BsubC.x));
			if (B_angle < 0)
				B_angle += 360;
			startAngle = Math.min(A_angle, B_angle);
			double endAngle = Math.max(A_angle, B_angle);
			angle = endAngle - startAngle;
			if (angle > 180) {
				angle = Math.abs(angle - 360);
				startAngle = endAngle;
			}
			
			// draw this angle on the chart
			Graphics2D g = image.createGraphics();
			int oldFrameIndex = frameIndex - 1;
			if (Double.isNaN(oldAngle) || oldFrameIndex < 0) {
				oldFrameIndex = frameIndex;
				oldAngle = angle;
			}
			//g.setColor(Color.BLACK);
			//g.drawRect(oldFrameIndex, 0, frameIndex - oldFrameIndex + 1, image.getHeight());
			g.setColor(color);
			g.setStroke(new BasicStroke(2));
			g.drawLine(graphX1 + oldFrameIndex, graphY1 + (int)MyUtil.lerp(0, 0, 180, graphHeight, oldAngle), graphX1 + frameIndex, graphY1 + (int)MyUtil.lerp(0, 0, 180, graphHeight, angle));
		}
		public void draw1(Graphics2D g) {
			drawLine(g, a.center, center, Color.BLACK, new BasicStroke(5));
			drawLine(g, center, b.center, Color.BLACK, new BasicStroke(5));
			int radius = 8;
			Point center = new Point();
			transform.transform(this.center, center);
			g.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
			
			
			/*
			g.setColor(color);
			g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			int radius = 12;
			Point center = new Point();
			transform.transform(this.center.center, center);
			
			if ((int)angle > 4) {
				g.drawArc(center.x - radius, center.y - radius, radius * 2, radius * 2, (int)startAngle, (int)angle);
			}
			*/
		}
		public void draw2(Graphics2D g) {
			g.setColor(color);
			g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			int radius = 17;
			Point center = new Point();
			transform.transform(this.center, center);
			
			if ((int)angle > 4) {
				g.drawArc(center.x - radius, center.y - radius, radius * 2, radius * 2, (int)startAngle, (int)angle);
			}
			
			
			drawLine(g, a.center, this.center, Color.GREEN, new BasicStroke(3));
			drawLine(g, this.center, b.center, Color.GREEN, new BasicStroke(3));
			radius = 8;
			center = new Point();
			transform.transform(this.center, center);
			g.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
		}
		public void drawLine(Graphics2D g, Point a, Point b, Color color, Stroke stroke) {
			g.setColor(color);
			g.setStroke(stroke);
			
			Point p1 = (Point)a.clone();
			Point p2 = (Point)b.clone();
			transform.transform(p1, p1);
			transform.transform(p2, p2);
			int x1 = p1.x;
			int y1 = p1.y;
			int x2 = p2.x;
			int y2 = p2.y;
			
			if (x1 == x2 && y1 == y2) {
				g.drawLine(x1, y1, x2, y2);
				return;
			}
			
			float dist = (float)Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
			float percent = 8.0f / dist;
			
			int dx = x2 - x1;
			int dy = y2 - y1;
			
			x1 += percent * dx;
			y1 += percent * dy;
			x2 -= percent * dx;
			y2 -= percent * dy;	
			
			g.drawLine(x1, y1, x2, y2);
		}
	}
	class Tracker {
		public Point originalCenter;
		
		public Point center;
		public int radius;
		public Color color;
		public double jointRadius;
		
		public Tracker(int x, int y) {
			originalCenter = new Point(x, y);
			center = originalCenter;
			radius = 20;
			color = new Color(frameImage.getRGB(x, y));
			update(frameImage);
			color = new Color(frameImage.getRGB(center.x, center.y));
			jointRadius = 50;
		}
		public void reset() {
			center = originalCenter;
			update(frameImage);
		}
		public void draw1(Graphics2D g) {
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(5));
			int radius = 8;
			Point center = new Point();
			transform.transform(this.center, center);
			g.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
		}
		public void draw2(Graphics2D g) {
			g.setColor(Color.GREEN);
			g.setStroke(new BasicStroke(3));
			int radius = 8;
			Point center = new Point();
			transform.transform(this.center, center);
			g.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
			
			/*
			radius = (int)jointRadius;
			g.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
			*/
		}
		
		public void update(BufferedImage img) {
			Point oldCenter = center;
			for (int safety = 0; safety < 10; safety++) {
				float avgX = 0.0f;
				float avgY = 0.0f;
				float avgCount = 0;
				for (int x = center.x - radius; x <= center.x + radius; x++) {
					for (int y = center.y - radius; y <= center.y + radius; y++) {
						if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight())
							continue;						
						
						Color c = new Color(img.getRGB(x, y));
						float w = (255.0f - ((float)colorDiff(color, c) / 3.0f)) / 255.0f;
						w = (float)Math.pow(w, 6);
						w *= 1.0f - ((float)Math.max(Math.abs(x - center.x), Math.abs(y - center.y)) / radius);
						if (w < 0.2f)
							w = 0.0f;
						
						avgX += w * x;
						avgY += w * y;
						avgCount += w;
					}
				}
				center = new Point((int)(avgX / avgCount), (int)(avgY / avgCount));
				if (center.equals(new Point(0, 0))) {
					radius = 40;
					center = oldCenter;
				} else if (oldCenter.equals(center)) {
					break;
				}
				oldCenter = center;
			}
			radius = 20;
		}
		public int colorDiff(Color a, Color b) {
			return Math.abs(a.getRed() - b.getRed()) + 
				Math.abs(a.getGreen() - b.getGreen()) +
				Math.abs(a.getBlue() - b.getBlue());
		}
	}
}
