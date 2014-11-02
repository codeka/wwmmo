package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.List;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Mesh;
import org.andengine.entity.scene.Scene;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Triangle;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;
import au.com.codeka.controlfield.ControlField;
import au.com.codeka.warworlds.model.Sector;

/** Represents the ControlField used by the tactical view. */
class TacticalControlField extends ControlField {
  private ArrayList<Mesh> meshes;

  public TacticalControlField(PointCloud pointCloud, Voronoi voronoi) {
    super(pointCloud, voronoi);
    meshes = new ArrayList<Mesh>();
  }

  public void updateAlpha(boolean visible, float alpha) {
    for (Mesh mesh : meshes) {
      mesh.setVisible(visible);
      mesh.setAlpha(alpha);
    }
  }

  public void addToScene(Scene scene, VertexBufferObjectManager vboManager, int colour,
      float alpha) {
    for (Vector2 pt : mOwnedPoints) {
      List<Triangle> triangles = mVoronoi.getTrianglesForPoint(pt);
      if (triangles == null) {
        continue;
      }

      float[] meshVertices = new float[(triangles.size() + 2) * Mesh.VERTEX_SIZE];
      meshVertices[Mesh.VERTEX_INDEX_X] = (float) pt.x * Sector.SECTOR_SIZE;
      meshVertices[Mesh.VERTEX_INDEX_Y] = (float) pt.y * Sector.SECTOR_SIZE;
      for (int i = 0; i < triangles.size(); i++) {
        meshVertices[(i + 1) * Mesh.VERTEX_SIZE + Mesh.VERTEX_INDEX_X] =
            (float) triangles.get(i).centre.x * Sector.SECTOR_SIZE;
        meshVertices[(i + 1) * Mesh.VERTEX_SIZE + Mesh.VERTEX_INDEX_Y] =
            (float) triangles.get(i).centre.y * Sector.SECTOR_SIZE;
      }
      meshVertices[(triangles.size() + 1) * Mesh.VERTEX_SIZE + Mesh.VERTEX_INDEX_X] =
          (float) triangles.get(0).centre.x * Sector.SECTOR_SIZE;
      meshVertices[(triangles.size() + 1) * Mesh.VERTEX_SIZE + Mesh.VERTEX_INDEX_Y] =
          (float) triangles.get(0).centre.y * Sector.SECTOR_SIZE;

      Mesh mesh = new Mesh(0.0f, 0.0f, meshVertices, triangles.size() + 2, DrawMode.TRIANGLE_FAN,
          vboManager);
      mesh.setColor(colour);
      mesh.setAlpha(alpha);
      scene.attachChild(mesh);
      meshes.add(mesh);
    }
  }
}
