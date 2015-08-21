import processing.core.PApplet;
import processing.core.PImage;
import processing.video.*;
import processing.serial.*;

public class DiscoMirror extends PApplet {
    public int _width = 640;
    public int _height = 360;
    public int div_x = 0;
    public int div_y = 0;
    public int div_width = 20;
    public int div_height = 20;
    public int div_size = 0;
    public int window_x_start = 0;
    public int window_x_end = 0;
    public float threshold = 30;
    public int time;
    public int refresh = 100;

    int blockColor;
    boolean debug = false;

    public byte[] matrix = {
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000
    };

    // Variable for capture device
    public Capture video;

    // Saved background
    PImage backgroundImage;

    // Adruino object for serial communication
    Serial port;


    public void setup() {
        size(_width, _height);

        window_x_start = ((width - height) / 2);
        window_x_end = (window_x_start + height);

        String[] cameras = Capture.list();
        println(cameras);

        video = new Capture(this, cameras[3]);
        video.start();

        // Create an empty image the same size as the video
        backgroundImage = createImage(width, height, RGB);

        div_x = height / div_width;
        div_y = height / div_height;
        div_size = div_x * div_y;
        println(div_x + "x" + div_y);

        println(Serial.list());

        try {
            port = new Serial(this, Serial.list()[5], 9600);
        } catch (Exception e) {
            System.out.println("SHIT:" + e);
        }

        textSize(55);
        fill(0, 0, 0);
        time = millis();

        blockColor = color((0), (255), (255));
    }

    public void draw() {
        // Capture video
        if (video.available()) {
            video.read();
        }

        // We are looking at the video's pixels, the memorized backgroundImage's pixels, as well as accessing the display pixels.
        // So we must loadPixels() for all!
        loadPixels();
        video.loadPixels();
        backgroundImage.loadPixels();

        // Begin loop to walk through every pixel
        processInput();
        updatePixels();

        if (debug) {
            text(threshold, 5, 50);
            text(frameRate, 1000, 50);
        }
        printMatrix(matrix);
        if (port != null) {
            port.write(matrix);
        }
        delay(refresh);
    }

    private void processInput() {
        int loc = 0;
        int mirrored_loc = 0;
        int column = 0;
        for (int x = window_x_start; x < window_x_end; x += div_x) {
            int row = 0;
            for (int y = 0; y < video.height; y += div_y) {
                int[] fg_colors = new int[div_size];
                int[] bg_colors = new int[div_size];
                int counter = 0;
                for (int temp_x = x; temp_x < x + div_x; temp_x++) {
                    for (int temp_y = y; temp_y < y + div_y; temp_y++) {
                        loc = temp_x + (temp_y * video.width);
                        fg_colors[counter] = video.pixels[loc];
                        bg_colors[counter] = backgroundImage.pixels[loc];
                        counter++;
                    }
                }

                int fgColor = averageColor(fg_colors);
                int bgColor = averageColor(bg_colors);

                // Step 4, compare the foreground and background color
                float r1 = red(fgColor);
                float g1 = green(fgColor);
                float b1 = blue(fgColor);
                float r2 = red(bgColor);
                float g2 = green(bgColor);
                float b2 = blue(bgColor);
                float diff = dist(r1, g1, b1, r2, g2, b2);

                // Step 5, Is the foreground color different from the background color
                for (int temp_x = x; temp_x < x + div_x; temp_x++) {
                    for (int temp_y = y; temp_y < y + div_y; temp_y++) {
                        loc = (temp_x) + (temp_y * video.width);
                        mirrored_loc = ((width - 1) - temp_x) + (width * temp_y);
                        if (diff > threshold) {
                            // If so, display the foreground color
                            pixels[mirrored_loc] = video.pixels[loc];
                        } else {
                            // If not, display block
                            pixels[mirrored_loc] = blockColor;
                        }
                    }
                }

                // Set the byte matrix
                setMatrix(row, ((div_width - 1) - column), diff > threshold);
                row++;
            }
            column++;
        }
    }

    private void setMatrix(int _row, int _col, boolean turnOn) {
        int index = (_col / 8) + (3 * _row);
        int bitIndex =  7 - (_col % 8);
        if (turnOn) {
            matrix[index] |= (1 << bitIndex);
        } else {
            matrix[index] &= ~(1 << bitIndex);
        }
        if (_row == 19 && turnOn && index == 59) {
            System.out.println(String.format("%8s", Integer.toBinaryString(matrix[59] & 0xFF)).replace(' ', '0'));
        }
    }

    public int averageColor(int[] colors) {
        float r = 0;
        float g = 0;
        float b = 0;

        for (int i = 0; i < colors.length; i++) {
            r += red(colors[i]);
            g += green(colors[i]);
            b += blue(colors[i]);
        }

        r = r / colors.length;
        g = g / colors.length;
        b = b / colors.length;

        return color(r, g, b);
    }

    public void mousePressed() {
        // Copying the current frame of video into the backgroundImage object
        // Note copy takes 5 arguments:
        // The source image
        // x,y,width, and height of region to be copied from the source
        // x,y,width, and height of copy destination
        backgroundImage.copy(video, 0, 0, video.width, video.height, 0, 0, video.width, video.height);
        backgroundImage.updatePixels();
    }

    public void keyPressed() {
        if (key == CODED) {
            if (keyCode == UP) {
                threshold += 5;
            } else if (keyCode == DOWN) {
                threshold -= 5;
            }
            if (threshold < 0) {
                threshold = 0;
            }
        } else {
            if (key == '[') {
                refresh--;
                if (refresh < 1) {
                    refresh = 1;
                }
            } else if (key == ']') {
                refresh++;
            }
        }
    }

    public void printMatrix(byte[] theMatrix) {
        int index = 0;
        for (byte b : theMatrix) {
            System.out.print(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            index++;
            if (index % 3 == 0) {
                System.out.println();
            }
        }
        System.out.println();
        System.out.println();
    }
}

