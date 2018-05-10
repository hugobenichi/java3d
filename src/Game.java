//
//  Experimenting with OpenGl and LWJGL
//  Based on ThinMatrix opengl tutorials videos
//


import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.PixelFormat;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

// Useful constants to avoid hardcoding mystical values in the middle of even more mystical argument lists.
interface K {

  int offset0 = 0;

  int attr0 = 0;

  int vertex_per_triangle = 3;

  int gl_null = 0;
}

// Static functions for reducing GL syntax bloat.
class GLUtil {
  // TODO: add debugging logging for tracking id/attrs binding and unbinding ops.

  static void vaoBind(int id) {
    GL30.glBindVertexArray(id);
  }

  static void vaoUnbind() {
    vaoBind(0);
  }

  static void vboArrayBufferBind(int id) {
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
  }

  static void vboArrayBufferUnbind() {
    vboArrayBufferBind(0);
  }

  static void vboElementArrayBufferBind(int id) {
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id);
  }

  static void vboElementArrayBufferUnbind() {
    vboElementArrayBufferBind(0);
  }

  static void vertexAttribArrayBind(int id) {
    GL20.glEnableVertexAttribArray(id);
  }

  static void vertexAttribArrayUnbind(int id) {
    GL20.glDisableVertexAttribArray(id);
  }

  static void attributeStore(int attrId, float[] data) {
    int vboId = GLObjects.allocVbo();
    vboArrayBufferBind(vboId);
    FloatBuffer buffer = BufferUtil.make(data);
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
    // TODO: separate vbo loading code above from attribute binding code below
    // TODO: what is this 'false' parameter ??
    GL20.glVertexAttribPointer(attrId, K.vertex_per_triangle, GL11.GL_FLOAT, false, K.gl_null, K.gl_null);
    vboArrayBufferUnbind();
  }

  static void bindIndices(int[] indices) {
    int vboId = GLObjects.allocVbo();
    vboElementArrayBufferBind(vboId);
    IntBuffer buffer = BufferUtil.make(indices);
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
    //vboElementArrayBufferUnbind(); // Why can't I unbind this ??
  }
}

// Static function for managing array of ints/floats
class BufferUtil {
  // TODO: consider pooling buffers

  static IntBuffer make(int[] data) {
    IntBuffer b = BufferUtils.createIntBuffer(data.length);
    b.put(data);
    b.flip();
    return b;
  }

  static FloatBuffer make(float[] data) {
    FloatBuffer b = BufferUtils.createFloatBuffer(data.length);
    b.put(data);
    b.flip();
    return b;
  }
}

// Game configs
interface Config {
  int WIDTH           = 1280;
  int HEIGHT          = 720;
  int FPS_CAP         = 60;
  String TITLE        = "Our First Display";
}

// Geometry data
interface Data {
  float[] vertices2 = {
      // Left bottom triangle
      -0.5f,      0.5f,       0f,
      -0.5f,      -0.5f,      0f,
      0.5f,       -0.5f,      0f,
      0.5f,       0.5f,       0f,
  };
  float[] vertices = {
      // Left bottom triangle
      -0.5f,      0.5f,       0f,
      -0.5f,      -0.5f,      0f,
      0.5f,       -0.5f,      0f,
      // Right top triangle
      0.5f,       -0.5f,      0f,
      0.5f,       0.5f,       0f,
      -0.5f,      0.5f,       0f
  };

  int[] indices = {
    0, 1, 3,
    3, 1, 2,
  };
}

// Used to track Vertex Array Object data.
class Model {
  int vaoId;
  int vertexCount;
}

// The main class
public class Game {

  public static void main(String[] args) throws Exception {


    // DISPLAY MANAGEMENT
    Display.setDisplayMode(new DisplayMode(Config.WIDTH, Config.HEIGHT));
    Display.create(new PixelFormat(), new ContextAttribs(3, 2).withForwardCompatible(true).withProfileCore(true));
    Display.setTitle(Config.TITLE);

    GL11.glViewport(0, 0, Config.WIDTH, Config.HEIGHT);


    // Geometry startup
    Model model = modelMake(Data.vertices, Data.indices);


    while (!Display.isCloseRequested()) {
      // TODO: get input
      // TODO: run game logic

      // Prepare rendering
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
      GL11.glClearColor(1, 0, 1, 1); // RGBA

      // Render models
      modelRender(model);

      // Display sync
      Display.sync(Config.FPS_CAP);
      Display.update();
    }


    // Cleanup
    GLObjects.freeAll();
    Display.destroy();
  }

  static Model modelMake(float[] positions, int[] indices) {
    int vaoId = GLObjects.allocVao();
    GLUtil.vaoBind(vaoId);
    GLUtil.bindIndices(indices);
    GLUtil.attributeStore(K.attr0, positions);
    GLUtil.vaoUnbind();
    Model m = new Model();
    m.vaoId = vaoId;
    m.vertexCount = indices.length;
    return m;
  }

  static void modelRender(Model model) {
    GLUtil.vaoBind(model.vaoId);
    GLUtil.vertexAttribArrayBind(K.attr0); // CLEANUP: hardcoded ! put this into the model instead
    //GL11.glDrawArrays(GL11.GL_TRIANGLES, K.offset0, model.vertexCount); // No indices rendering
    GL11.glDrawElements(GL11.GL_TRIANGLES, model.vertexCount, GL11.GL_UNSIGNED_INT, K.offset0); // Indices rendering
    GLUtil.vertexAttribArrayUnbind(K.attr0);
    GLUtil.vaoUnbind();
  }
}


// Tracks VBO and VAO ids
class GLObjects {
  // TODO: tracks ids more efficiently without boxing ids !
  static List<Integer> vaos = new ArrayList<Integer>();
  static List<Integer> vbos = new ArrayList<Integer>();

  static int allocVao() {
    int id = GL30.glGenVertexArrays();
    vaos.add(id);
    return id;
  }

  static int allocVbo() {
    int id = GL15.glGenBuffers();
    vbos.add(id);
    return id;
  }

  static void freeAll() {
    vaos.forEach(GL30::glDeleteVertexArrays);
    vbos.forEach(GL15::glDeleteBuffers);
  }
}
