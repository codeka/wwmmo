package au.com.codeka.planetrender;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TextureGenerator {
    private Generator mGenerator;

    public TextureGenerator(Template.TextureTemplate tmpl, Random rand) {
        if (tmpl.getGenerator() == Template.TextureTemplate.Generator.VoronoiMap) {
            mGenerator = new VoronoiMapGenerator(tmpl, rand);
        }
    }

    /**
     * Gets the colour of the texel at the given (u,v) coordinates.
     */
    public Colour getTexel(double u, double v) {
        return mGenerator.getTexel(u, v);
    }

    /**
     * Renders the complete texture to the given \c Image, mostly useful for debugging.
     */
    public void renderTexture(Image img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                double u = (double) x / (double) img.getWidth();
                double v = (double) y / (double) img.getHeight();
                img.setPixelColour(x, y, getTexel(u, v));
            }
        }
    }

    static abstract class Generator {
        public abstract Colour getTexel(double u, double v);
    }

    /**
     * This generator just returns a colour based on how from the points the texel is.
     */
    static class VoronoiMapGenerator extends Generator {
        Voronoi mVoronoi;
        ColourGradient mColourGradient;

        public VoronoiMapGenerator(Template.TextureTemplate tmpl, Random rand) {
            Template.VoronoiTemplate voronoiTmpl = tmpl.getParameter(Template.VoronoiTemplate.class);
            mVoronoi = new Voronoi(voronoiTmpl, rand);
            mColourGradient = tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
        }

        public Colour getTexel(double u, double v) {
            final Vector2 uv = new Vector2(u, v);
            final Vector2 pt = mVoronoi.findClosestPoint(uv);

            // find the closest neighbour
            Vector2 neighbour = null;
            double neighbourDistance2 = 1.0;
            Set<Vector2> neighbours = mVoronoi.getNeighbours(pt);
            if (neighbours == null) {
                neighbours = new HashSet<Vector2>();
            }

            for (Vector2 n : neighbours) {
                if (neighbour == null) {
                    neighbour = n;
                    neighbourDistance2 = uv.distanceTo2(n);
                } else {
                    double distance2 = uv.distanceTo2(n);
                    if (distance2 < neighbourDistance2) {
                        neighbour = n;
                        neighbourDistance2 = distance2;
                    }
                }
            }

            final double neighbourDistance = Math.sqrt(neighbourDistance2);
            final double distance = uv.distanceTo(pt);
            final double totalDistance = distance + neighbourDistance;

            double normalizedDistance = distance / (totalDistance / 2.0);
            return mColourGradient.getColour(normalizedDistance);
        }
    }
}
