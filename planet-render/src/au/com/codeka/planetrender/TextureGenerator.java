package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import au.com.codeka.common.Colour;
import au.com.codeka.common.ColourGradient;
import au.com.codeka.common.Image;
import au.com.codeka.common.PerlinNoise;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;

public class TextureGenerator {
    private Generator mGenerator;
    private double mScaleX;
    private double mScaleY;

    public TextureGenerator(Template.TextureTemplate tmpl, Random rand) {
        if (tmpl.getGenerator() == Template.TextureTemplate.Generator.VoronoiMap) {
            mGenerator = new VoronoiMapGenerator(tmpl, rand);
        } else if (tmpl.getGenerator() == Template.TextureTemplate.Generator.PerlinNoise) {
            mGenerator = new PerlinNoiseGenerator(tmpl, rand);
        }

        mScaleX = tmpl.getScaleX();
        mScaleY = tmpl.getScaleY();
    }

    /**
     * Gets the colour of the texel at the given (u,v) coordinates.
     */
    public Colour getTexel(double u, double v) {
        return mGenerator.getTexel(u * mScaleX, v * mScaleY);
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
        private Voronoi mVoronoi;
        private ColourGradient mColourGradient;
        private PerlinNoise mNoise;
        private double mNoisiness;

        public VoronoiMapGenerator(Template.TextureTemplate tmpl, Random rand) {
            Template.VoronoiTemplate voronoiTmpl = tmpl.getParameter(Template.VoronoiTemplate.class);
            mVoronoi = new TemplatedVoronoi(voronoiTmpl, rand);
            mColourGradient = tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
            mNoisiness = tmpl.getNoisiness();

            Template.PerlinNoiseTemplate noiseTemplate = tmpl.getParameter(Template.PerlinNoiseTemplate.class);
            if (noiseTemplate != null) {
                mNoise = new TemplatedPerlinNoise(noiseTemplate, rand);
            }
        }

        public Colour getTexel(double u, double v) {
            final Vector2 uv = Vector2.pool.borrow().reset(u, v);
            final Vector2 pt = mVoronoi.findClosestPoint(uv);

            // find the closest neighbour
            Vector2 neighbour = null;
            double neighbourDistance2 = 1.0;
            List<Vector2> neighbours = mVoronoi.getNeighbours(pt);
            if (neighbours == null) {
                neighbours = new ArrayList<Vector2>();
            }

            int num = neighbours.size();
            for (int i = 0; i < num; i++) {
                Vector2 n = neighbours.get(i);
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
            if (mNoise != null) {
                double noise = mNoise.getNoise(u, v);
                normalizedDistance += (mNoisiness / 2.0) - (noise * mNoisiness);
            }

            Vector2.pool.release(uv);
            return mColourGradient.getColour(normalizedDistance);
        }
    }

    /**
     * A texture generator that generates textures based on perlin noise.
     */
    static class PerlinNoiseGenerator extends Generator {
        private PerlinNoise mNoise;
        private ColourGradient mColourGradient;

        public PerlinNoiseGenerator(Template.TextureTemplate tmpl, Random rand) {
            mNoise = new TemplatedPerlinNoise(tmpl.getParameter(Template.PerlinNoiseTemplate.class), rand);
            mColourGradient = tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
        }

        public Colour getTexel(double u, double v) {
            final double noise = mNoise.getNoise(u, v);
            return mColourGradient.getColour(noise);
        }
    }
}
