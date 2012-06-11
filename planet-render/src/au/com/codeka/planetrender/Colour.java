package au.com.codeka.planetrender;

/**
 * Helper class that represents an ARGB colour.
 */
public class Colour {
    public int argb;

    public Colour() {
        argb = 0x00000000;
    }
    public Colour(int argb) {
        this.argb = argb;
    }

    public void setAlpha(double a) {
        int n = (int)(255 * a);
        argb = (n << 24) | (argb & 0x00ffffff);
    }
    public void setRed(double r) {
        int n = (int)(255 * r);
        argb = (n << 16) | (argb & 0xff00ffff);
    }
    public void setGreen(double g) {
        int n = (int)(255 * g);
        argb = (n << 8) | (argb & 0xffff00ff);
    }
    public void setBlue(double b) {
        int n = (int)(255 * b);
        argb = n | (argb & 0xffffff00);
    }

    public static Colour RED = new Colour(0xffff0000);
    public static Colour GREEN = new Colour(0xff00ff00);
    public static Colour BLUE = new Colour(0xff0000ff);
    public static Colour WHITE = new Colour(0xffffffff);
    public static Colour BLACK = new Colour(0xff000000);
    public static Colour TRANSPARENT = new Colour(0x00000000);
}
