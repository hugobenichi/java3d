//
//  Experimenting with OpenGl and LWJGL
//  Based on ThinMatrix opengl tutorials videos
//


import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// The main class
public final class Game {

  static final Matrix4f proj =
      VecUtil.projectionMatrix(Config.PROJECTION_FOV, Config.PROJECTION_NEAR, Config.PROJECTION_FAR);

  public static void main(String[] args) throws Exception {


    // DISPLAY MANAGEMENT
    Display.setDisplayMode(new DisplayMode(Config.WIDTH, Config.HEIGHT));
    Display.create(new PixelFormat(), new ContextAttribs(3, 2).withForwardCompatible(true).withProfileCore(true));
    Display.setTitle(Config.TITLE);

    GL11.glViewport(0, 0, Config.WIDTH, Config.HEIGHT);


    // Shader loading; touch all shader subclasses to force static shader loading code.
    Shader text = Shader.Tex.s;
    Shader grad = Shader.Gradient.s;
    Shader s = text; //grad;

    // Texture loading
    Texture tex = Texture.test_texture;

    // Geometry startup

    // TODO: Model should store the texture, and maybe the shader too.
    Model model = modelMake(Data.vertices, Data.indices, Data.tex_uvs);

    Thing thing = new Thing(0,0,0);

    while (!Display.isCloseRequested()) {
      // TODO: get input
      Input.process();

      // TODO: run game logic
      //thing.move(0.001f, 0.001f, 1.0f);
      //thing.rot(0.01f);

      // Prepare rendering
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glClearColor(1, 0, 1, 1);

      // Draw stuff
      //modelRender(model, tex, s);
      thing.render();

      // Display sync
      Display.sync(Config.FPS_CAP);
      Display.update();
    }

    // Cleanup
    Shader.freeAll();
    GLObjects.freeAll();
    Display.destroy();
  }

  static Model modelMake(float[] positions, int[] indices, float[] uvs) {
    int vertexAttr = 0;
    int uvsAttr = 1;


    int vaoId = GLObjects.allocVao();
    GLUtil.vaoBind(vaoId);
    GLUtil.bindIndices(indices);
    GLUtil.attributeStore(K.attr0, K.float_per_vertex, positions);
    GLUtil.attributeStore(K.attr1, K.float_per_uv, uvs);
    GLUtil.vaoUnbind();
    Model m = new Model();
    m.vaoId = vaoId;
    m.vertexCount = indices.length;
    return m;
  }

  static void modelRender(Model model, Texture tex, Shader s) {
    Shader.use(s);
    GLUtil.vaoBind(model.vaoId);
    GLUtil.vertexAttribArrayBind(K.attr0); // CLEANUP: hardcoded ! put this into the model instead
    GLUtil.vertexAttribArrayBind(K.attr1);
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GLUtil.textureBind(tex.texId);
    GL11.glDrawElements(GL11.GL_TRIANGLES, model.vertexCount, GL11.GL_UNSIGNED_INT, K.offset0);
    GLUtil.vertexAttribArrayUnbind(K.attr0);
    GLUtil.vertexAttribArrayUnbind(K.attr1);
    GLUtil.vaoUnbind();
    Shader.stop();
  }
}


// Useful constants to avoid hardcoding mystical values in the middle of even more mystical argument lists.
interface K {

  boolean debug = false;

  boolean no_transpose = false;

  int gl_null = 0;
  int offset0 = 0;

  int attr0 = 0;
  int attr1 = 1;

  int float_per_vertex = 3;
  int float_per_uv = 2;
}


// Static functions for reducing GL syntax bloat.
final class GLUtil {
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

  static void textureBind(int id) {
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
  }

  static void textureUnbind() {
    textureBind(0);
  }

  static void attributeStore(int attrId, int attrSize, float[] data) {
    int vboId = GLObjects.allocVbo();
    vboArrayBufferBind(vboId);
    FloatBuffer buffer = BufferUtil.make(data);
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
    // TODO: separate vbo loading code above from attribute binding code below
    // TODO: what is this 'false' parameter ??
    GL20.glVertexAttribPointer(attrId, attrSize, GL11.GL_FLOAT, false, K.gl_null, K.gl_null);
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


// Tracks VBO and VAO ids
final class GLObjects {
  // TODO: tracks ids more efficiently without boxing ids !
  static final List<Integer> vaos = new ArrayList<Integer>();
  static final List<Integer> vbos = new ArrayList<Integer>();
  static final List<Integer> textures = new ArrayList<Integer>();

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

  static int allocTexture() {
    int id = GL11.glGenTextures();
    textures.add(id);
    return id;
  }

  static void freeAll() {
    vaos.forEach(GL30::glDeleteVertexArrays);
    vbos.forEach(GL15::glDeleteBuffers);
    textures.forEach(GL11::glDeleteTextures);
  }
}


// Static function for managing array of ints/floats
final class BufferUtil {
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


final class IOUtil {
  static String readFile(String path) {
    try {
      byte[] data = Files.readAllBytes(Paths.get(path));
      return new String(data, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}


// Game configs
interface Config {
  int WIDTH           = 1280;
  int HEIGHT          = 720;
  int FPS_CAP         = 60;
  String TITLE        = "Game";

  float PROJECTION_FOV  = 90;
  float PROJECTION_NEAR = 1f;
  float PROJECTION_FAR  = 1000f;
}


// Geometry data
interface Data {
  float[] vertices = {
    -0.5f,      0.5f,       0f,       // v0: top left
    0.5f,       0.5f,       0f,       // v1: top right
    0.5f,       -0.5f,      0f,       // v2: bot right
    -0.5f,      -0.5f,      0f,       // v3: bot left
  };

  int[] indices = {
    0, 3, 1,  // upper left triangle
    1, 3, 2,  // lower right triangle
  };

  float[] tex_uvs = { // same orders as vertices
    0.0f,    0.0f,
    1.0f,    0.0f,
    1.0f,    1.0f,
    0.0f,    1.0f,
  };
}


// Used to track Vertex Array Object data.
final class Model {
  int vaoId;
  int vertexCount;
}


final class Shader {
  int programId;
  int vertexId;
  int fragmentId;
  String[] bindings;

  static final String SKIP = "SKIP_BINDING";
  static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(4 * 4); // !! not thread safe obviously !!

  // Shader creation

  static Shader make(String shadername, String... bindings) {
    Shader s = new Shader();
    s.vertexId = loadShader("./src/shaders/" + shadername + ".vs", GL20.GL_VERTEX_SHADER);
    s.fragmentId = loadShader("./src/shaders/" + shadername + ".fs", GL20.GL_FRAGMENT_SHADER);
    s.programId = GL20.glCreateProgram();
    s.bindings = bindings;
    GL20.glAttachShader(s.programId, s.vertexId);
    GL20.glAttachShader(s.programId, s.fragmentId);
    for (int i = 0; i < s.bindings.length; i++) {
      if (s.bindings[i] == SKIP) {
        continue;
      }
      if (K.debug) System.out.println(String.format("binding attr %d to '%s'", i, s.bindings[i]));
      GL20.glBindAttribLocation(s.programId, i, s.bindings[i]);
    }
    GL20.glLinkProgram(s.programId);
    GL20.glValidateProgram(s.programId);
    shaders.add(s);
    return s;
  }

  static int loadShader(String filepath, int type) {
    String source = IOUtil.readFile(filepath);
    int id = GL20.glCreateShader(type);
    GL20.glShaderSource(id, source);
    GL20.glCompileShader(id);
    if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
      throw new RuntimeException(String.format("Could not compiler shader %s: %s", filepath, GL20.glGetShaderInfoLog(id, 500)));
    }
    return id;
  }

  // Uniform variable loading

  static int locationOf(Shader s, String varName) {
    return GL20.glGetUniformLocation(s.programId, varName);
  }

  static void load1f(int loc, float x) {
    GL20.glUniform1f(loc, x);
  }

  static void load2f(int loc, float x, float y) {
    GL20.glUniform2f(loc, x, y);
  }

  static void load3f(int loc, float x, float y, float z) {
    GL20.glUniform3f(loc, x, y, z);
  }

  static void loadVec3f(int loc, Vector3f v) {
    GL20.glUniform3f(loc, v.x, v.y, v.z);
  }

  static void loadMat4f(int loc, Matrix4f m) {
    MATRIX_BUFFER.clear();
    m.store(MATRIX_BUFFER);
    MATRIX_BUFFER.flip();
    GL20.glUniformMatrix4(loc, K.no_transpose, MATRIX_BUFFER);
  }

  // Shader management

  static void use(Shader s) {
    GL20.glUseProgram(s.programId);
    for (int i = 0; i < s.bindings.length; i++) {
      if (s.bindings[i] == SKIP) {
        continue;
      }
      if (K.debug) System.out.println(String.format("binding attr %d to '%s'", i, s.bindings[i]));
      GL20.glBindAttribLocation(s.programId, i, s.bindings[i]);
    }
  }

  static void stop() {
    GL20.glUseProgram(0);
  }

  // Statically load all shaders
  static final List<Shader> shaders = new ArrayList<>();

  static void freeAll() {
    for (Shader s : shaders) {
      GL20.glDetachShader(s.programId, s.vertexId);
      GL20.glDetachShader(s.programId, s.fragmentId);
      GL20.glDeleteShader(s.vertexId);
      GL20.glDeleteShader(s.fragmentId);
      GL20.glDeleteShader(s.programId);
    }
  }


  // Individual shaders are declared and loaded in their own static classes.
  // This offers a place for managing the uniform variable locations without resorting to subclassing.
  // Everything ends up being static and final, which is perfect for JIT inlining.

  static final class Tex {
    static final Shader s = Shader.make("tex", "position", "uv");
  }

  static final class Gradient {
    static final Shader s = Shader.make("gradient");

    static final int loc_transformation = Shader.locationOf(s, "transformation");
    static final int loc_projection = Shader.locationOf(s, "projection");

    static void loadTransformtion(Matrix4f m) {
      Shader.loadMat4f(loc_transformation, m);
    }

    static void loadProjection(Matrix4f m) {
      Shader.loadMat4f(loc_projection, m);
    }

    static {
      Shader.use(s);
      loadProjection(Game.proj);
      Shader.stop();
    }
  }
}


final class PixelUtil {
  // Getters
  static int a(int rgba) { return 0xff & (rgba >> 24); }
  static int r(int rgba) { return 0xff & (rgba >> 16); }
  static int g(int rgba) { return 0xff & (rgba >>  8); }
  static int b(int rgba) { return 0xff & rgba; }

  static int rgba(int r, int g, int b, int a) {
    return (a << 24) | (r << 16) | (g << 8) | b;
  }
}


final class Texture {

  int texId;
  int w;
  int h;

  static Texture create(int w, int h, int[] pixels) {
    int len = w * h;

    ByteBuffer buffer = BufferUtils.createByteBuffer(len * 4);
    for (int pixel : pixels) {
      buffer.put((byte) PixelUtil.r(pixel));
      buffer.put((byte) PixelUtil.g(pixel));
      buffer.put((byte) PixelUtil.b(pixel));
      buffer.put((byte) PixelUtil.a(pixel));
    }
    buffer.flip();

    Texture t = new Texture();
    t.texId = GLObjects.allocTexture();
    t.w = w;
    t.h = h;

    GLUtil.textureBind(t.texId);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

    return t;
  }

  // A 32x32 texture made of 4 unicolor squares. TL: red, TR: green, BL: blue, BR: purple.
  static int[] testPixels() {
    int w = 32;
    int h = 32;
    int l = w * h;
    int[] pixels = new int[l];

    int i = 0;
    for (; i < 16; i++) {
      int j = 0;
      for (; j < 16; j++) {
        pixels[i * 32 + j] = PixelUtil.rgba(0xff, 0, 0, 0xff);
      }
      for (; j < 32; j++) {
        pixels[i * 32 + j] = PixelUtil.rgba(0, 0xff, 0, 0xff);
      }
    }
    for (; i < 32; i++) {
      int j = 0;
      for (; j < 16; j++) {
        pixels[i * 32 + j] = PixelUtil.rgba(0, 0, 0xff, 0xff);
      }
      for (; j < 32; j++) {
        pixels[i * 32 + j] = PixelUtil.rgba(0x80, 0, 0x80, 0xff);
      }
    }

    return pixels;
  }

  static int[] testPixels2() {
    int w = 32;
    int h = 32;
    int l = w * h;
    int[] pixels = new int[l];

    for (int i = 0; i < pixels.length; i++) {
      int a = 0xFF;
      int r = 0xFF & (i * 5);
      int g = 0xFF & (i * 2);
      int b = 0xFF & (i * 1);
      pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
    }

    return pixels;
  }

  static final Texture test_texture = Texture.create(32, 32, Texture.testPixels());
}

final class VecUtil {

  static void transformationMatrix(Matrix4f out, Vector3f trans, float rx, float ry, float rz, float scale) {
    out.setIdentity();
    Matrix4f.translate(trans, out, out);
    Matrix4f.rotate((float) Math.toRadians(rx), new Vector3f(1, 0, 0), out, out);
    Matrix4f.rotate((float) Math.toRadians(ry), new Vector3f(0, 1, 0), out, out);
    Matrix4f.rotate((float) Math.toRadians(rz), new Vector3f(0, 0, 1), out, out);
    Matrix4f.scale(new Vector3f(scale, scale, scale), out, out);
  }

  static Matrix4f projectionMatrix(float fov, float near, float far) {
    float w = (float) Display.getWidth();
    float h = (float) Display.getHeight();
    float a = w / h;
    float len = far - near;
    float scale = 1.0f / (float) Math.tan(Math.toRadians(fov / 2));

    Matrix4f proj = new Matrix4f();
    proj.m00 = scale;
    proj.m11 = scale * a;
    proj.m22 = - (near + far) / len;
    proj.m33 = 0;
    proj.m23 = -1;
    proj.m32 = - 2 * near * far / len;

    proj.setIdentity(); // BUG: my projection matrix is probably completely wrong ??
    return proj;
  }
}

final class Thing {
  Model model   = Game.modelMake(Data.vertices, Data.indices, Data.tex_uvs);
  Texture tex   = Texture.test_texture;
  Shader shader = Shader.Gradient.s;

  Vector3f pos =
      new Vector3f(0, 0, 0);
      //new Vector3f(-0.4f,0.8f,0);
  float rx;
  float ry;
  float rz; //= 34;
  float scale =
    1.0f;
    //0.3f;
  Matrix4f transformation = new Matrix4f();

  Thing(float x0, float y0, float z0) {
    move(x0, y0, z0);
    updateTransformation();
  }

  void move(float dx, float dy, float dz) {
    pos.x += dx;
    pos.y += dy;
    pos.z += dz;
    updateTransformation();
  }

  void rot(float drz) {
      rz += drz;
  }

  void updateTransformation() {
    VecUtil.transformationMatrix(transformation, pos, rx, ry, rz, scale);
  }

  void render() {
    Shader.use(shader);
    GLUtil.vaoBind(model.vaoId);
    GLUtil.vertexAttribArrayBind(K.attr0); // CLEANUP: hardcoded ! put this into the model instead
    GLUtil.vertexAttribArrayBind(K.attr1);
    Shader.Gradient.loadTransformtion(transformation);
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GLUtil.textureBind(tex.texId);
    GL11.glDrawElements(GL11.GL_TRIANGLES, model.vertexCount, GL11.GL_UNSIGNED_INT, K.offset0);
    GLUtil.vertexAttribArrayUnbind(K.attr0);
    GLUtil.vertexAttribArrayUnbind(K.attr1);
    GLUtil.vaoUnbind();
    Shader.stop();
  }
}

final class Input {

  static final int[] arrow_keys = {
    Keyboard.KEY_W,
    Keyboard.KEY_A,
    Keyboard.KEY_S,
    Keyboard.KEY_D,
  };

  static void process() {
    for (int k : arrow_keys) {
      if (Keyboard.isKeyDown(k)) {
        System.out.println("keydown: " + k);
      }
    }
  }
}
