import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class ImageLoader {
	
	BufferedImage image;

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		MyFrame frame = new MyFrame();
		frame.setVisible(true);
		
		MyFrame rawFrame = new MyFrame();
		rawFrame.setVisible(true);

		//Read image into mat
//		VideoCapture camera = new VideoCapture("http://192.168.10.22/axis-cgi/mjpg/video.cgi?dummy=param.mjpg");
		VideoCapture camera = new VideoCapture(0);
		if(camera.isOpened()) {
			System.out.println("Camera connected");
		}
		else {
			System.out.println("Camera not connected");
			return;
		}
		
		camera.set(Videoio.CAP_PROP_EXPOSURE, 5);
		
		long framerate = 1000 / 7;
	    // time the frame began. Edit the second value (60) to change the prefered FPS (i.e. change to 50 for 50 fps)
	    long frameStart;
	    // number of frames counted this second
	    long frameCount = 0;
	    // time elapsed during one frame
	    long elapsedTime;
	    // accumulates elapsed time over multiple frames
	    long totalElapsedTime = 0;
	    // the actual calculated framerate reported
		
		while(true) {
			frameStart = System.currentTimeMillis();
			
			Mat rawImage = new Mat();
			if(!camera.read(rawImage)) {
				System.out.println("Failed to read image");
				break;
			}
//			Mat rawImage = imread("/Users/griffen/Documents/Development/LoadImage/RealFullField/492.jpg");
			
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
			Imgproc.drawContours(rawImage, contourMOP, -1, new Scalar(0, 0, 255));
			Imgproc.drawContours(rawImage, hullsMOP, -1, new Scalar(255, 0, 0));
			Imgproc.fillPoly(rawImage, targets, new Scalar(0, 255, 0));
	        
			//Buffer the output
//			frame.render(contouredImage);
			rawFrame.render(rawImage);
			
			// calculate the time it took to render the frame
            elapsedTime = System.currentTimeMillis() - frameStart;
            // sync the framerate
            try {
                // make sure framerate milliseconds have passed this frame
                if (elapsedTime < framerate) {
                    Thread.sleep(framerate - elapsedTime);
                }
                else {
                    // don't starve the garbage collector
                    Thread.sleep(5);
                }
            }
            catch (InterruptedException e) {
                break;
            }
            ++frameCount;
            totalElapsedTime += (System.currentTimeMillis() - frameStart);
            if (totalElapsedTime > 1000) {
                long reportedFramerate = (long) ((double) frameCount / (double) totalElapsedTime * 1000.0);
                // show the framerate in the applet status window
                System.out.println("fps: " + reportedFramerate);
                // repaint();
                frameCount = 0;
                totalElapsedTime = 0;
            }
		}
		camera.release();
	}
	

}