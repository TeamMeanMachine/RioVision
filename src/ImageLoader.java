import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

public class ImageLoader {
	
	BufferedImage image;

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		//Create Axis Camera instance
		AxisCamera aCam = new AxisCamera("192.168.1.105");
		
		//Just for testing, use proper SmartDashboard on robot
		NetworkTable.setServerMode();
		NetworkTable.initialize();
		NetworkTable smartDashboard = NetworkTable.getTable("SmartDashboard");
		smartDashboard.putNumber("Brightness", aCam.getBrightness());
		smartDashboard.putNumber("Contrast", aCam.getContrast());
		smartDashboard.putNumber("Exposure", aCam.getExposure());
		
		//Camera properties change listeners
		smartDashboard.addTableListener("Brightness", new ITableListener() {
			
			@Override
			public void valueChanged(ITable source, String key, Object value, boolean isNew) {
				aCam.setBrightness(((Double) value).intValue()); 
			}
		}, true);
		
		smartDashboard.addTableListener("Contrast", new ITableListener() {
			
			@Override
			public void valueChanged(ITable source, String key, Object value, boolean isNew) {
				aCam.setContrast(((Double) value).intValue()); 
			}
		}, true);

		smartDashboard.addTableListener("Exposure", new ITableListener() {
	
			@Override
			public void valueChanged(ITable source, String key, Object value, boolean isNew) {
				aCam.setExposure(((Double) value).intValue()); 
			}
		}, true);

		//Open Axis camera
		VideoCapture camera = new VideoCapture(aCam.getVideoURL());
		
		if(camera.isOpened()) {
			System.out.println("Camera connected");
		}
		else {
			System.out.println("Camera not connected");
			return;
		}
		
		//Start MJPG server
		MjpgServer server = new MjpgServer();
		
		int frameWidth = (int) camera.get(Videoio.CAP_PROP_FRAME_WIDTH);
		int frameHeight = (int) camera.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		
		//Processing loop
		while(true) {		
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Grab frame from camera
			Mat rawImage = new Mat();
//			rawImage = imread("/Users/griffen/Documents/Development/RealFullField/7.jpg");
			if(!camera.read(rawImage)) {
				System.out.println("Failed to read image");
				continue;
			}
			
			// Filter image
			Mat filteredImage = new Mat();
			Core.inRange(rawImage, new Scalar(0, 125, 0), new Scalar(200, 255, 200), filteredImage);
				
			// Dilate monochrome
			Mat dilatededImage = new Mat();
			Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
			Imgproc.dilate(filteredImage, dilatededImage, element);

			// Find contours
			ArrayList<MatOfPoint> contourMOP = new ArrayList<MatOfPoint>();
			Imgproc.findContours(dilatededImage, contourMOP, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

			// Make them convex
			ArrayList<MatOfInt> hulls = new ArrayList<MatOfInt>(contourMOP.size());
			for (int i = 0; i < contourMOP.size(); i++) {
				hulls.add(new MatOfInt());
				Imgproc.convexHull(contourMOP.get(i), hulls.get(i), false);
			}

			// Create convex contours
			ArrayList<Point[]> hullContourPoints = new ArrayList<Point[]>(contourMOP.size());
			for (int nContour = 0; nContour < contourMOP.size(); nContour++) {
				double nLengthOfHull = hulls.get(nContour).size().height;
				Point[] hullPoints = new Point[hulls.get(nContour).rows()];

				// Loop over all points in current hull
				for (int nHullIndex = 0; nHullIndex < nLengthOfHull; nHullIndex++) {
					// Retrieve index of contour point on hull
					int nContourIndex = (int) hulls.get(nContour).get(nHullIndex, 0)[0];
					// Build an array of points for the current hull
					hullPoints[nHullIndex] = new Point(contourMOP.get(nContour).get(nContourIndex, 0)[0],
							contourMOP.get(nContour).get(nContourIndex, 0)[1]);
				}
				// Add hull to the array list (containing other hulls)
				hullContourPoints.add(hullPoints);
			}

			// Convert Point arrays into MatOfPoint
			ArrayList<MatOfPoint> hullsMOP = new ArrayList<MatOfPoint>();
			for (int i = 0; i < hullContourPoints.size(); i++) {
				MatOfPoint mop = new MatOfPoint();
				mop.fromArray(hullContourPoints.get(i));
				hullsMOP.add(mop);
			}

			// Compare areas of contours
			ArrayList<MatOfPoint> targets = new ArrayList<MatOfPoint>();
			for (int i = 0; i < hullsMOP.size(); i++) {
				double sizeConvex = Imgproc.contourArea(hullsMOP.get(i));
				double sizeConcave = Imgproc.contourArea(contourMOP.get(i));
				double concavity = sizeConvex / sizeConcave;
				if (concavity > 1.8 && sizeConvex > 1500) {
					targets.add(contourMOP.get(i));
				}
			}

			// Draw output
//			Mat contouredImage = new Mat(rawImage.size(), rawImage.type());
			Imgproc.fillPoly(rawImage, targets, new Scalar(0, 255, 0));
			Imgproc.drawContours(rawImage, contourMOP, -1, new Scalar(0, 0, 255), 2);
			Imgproc.drawContours(rawImage, hullsMOP, -1, new Scalar(255, 0, 0), 2);
			
			int targetCount = 0;
			
			for(MatOfPoint mop : targets) {
				MatOfPoint2f curve = new MatOfPoint2f();
				MatOfPoint2f approxCurve = new MatOfPoint2f();
				mop.convertTo(curve, CvType.CV_32FC2);
				Imgproc.approxPolyDP(curve, approxCurve, Imgproc.arcLength(curve, true) * 0.01, true);
				approxCurve.convertTo(mop, CvType.CV_32S);
				if(mop.rows() == 8) {
					targetCount++;
					int centerX = 0;
					for(int i = 0; i < mop.rows(); i++) {
						centerX += mop.get(i, 0)[0];
					}
					centerX /= mop.rows();
					int pixelOffset = Math.abs(centerX - (frameWidth / 2));
					Imgproc.arrowedLine(rawImage, new Point(frameWidth / 2, frameHeight / 2), new Point(centerX, frameHeight / 2), new Scalar(0, 0, 255), 10, 8, 0, 20.0 / pixelOffset);
//					Imgproc.line(rawImage, new Point(centerX - 50, centerY), new Point(centerX + 50, centerY), new Scalar(0, 0, 255), 2);
				}
			}
			smartDashboard.putNumber("TargetCount", targetCount);
	        
			//Send output to MJPG server
			server.sendFrame(toBufferedImage(rawImage));
		}
	}
	
	public static BufferedImage toBufferedImage(Mat m){
	        // Code from http://stackoverflow.com/questions/15670933/opencv-java-load-image-to-gui
	
	        // Check if image is grayscale or color
	    int type = BufferedImage.TYPE_BYTE_GRAY;
	    if ( m.channels() > 1 ) {
	        type = BufferedImage.TYPE_3BYTE_BGR;
	    }
	
	        // Transfer bytes from Mat to BufferedImage
	    int bufferSize = m.channels()*m.cols()*m.rows();
	    byte [] b = new byte[bufferSize];
	    m.get(0,0,b); // get all the pixels
	    BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
	    final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	    System.arraycopy(b, 0, targetPixels, 0, b.length);
	    return image;
	}

}