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


/* TODOs:
 *  - fix the translation / projection order issue:
 *       I need to apply a static z translation first, then projection, then translation again ?
 *  - add texture tilling
 */


// The main class
public final class Game {

  static final Matrix4f proj =
      //VecUtil.projectionMatrix2(Config.PROJECTION_FOV, Config.PROJECTION_NEAR, Config.PROJECTION_FAR);
      VecUtil.projectionMatrix(Config.PROJECTION_FOV, Config.PROJECTION_NEAR, Config.PROJECTION_FAR);

  public static void main(String[] args) throws Exception {


    // DISPLAY MANAGEMENT
    Display.setDisplayMode(new DisplayMode(Config.WIDTH, Config.HEIGHT));
    Display.create(new PixelFormat(), new ContextAttribs(3, 2).withForwardCompatible(true).withProfileCore(true));
    Display.setTitle(Config.TITLE);

    GL11.glViewport(0, 0, Config.WIDTH, Config.HEIGHT);

    // Texture loading
    Texture tex = Texture.test_texture;

    // Geometry startup
    Mesh mesh = Mesh.load(Data.vertices, Data.indices, Data.tex_uvs);

    float x = 0;
    float y = 0;
    float z = 0;

    float s = 0.1f;

    while (!Display.isCloseRequested()) {
      // Process input
      Input.process();
      if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))  { x -= 0.05f; }
      if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) { x += 0.05f; }
      if (Keyboard.isKeyDown(Keyboard.KEY_UP))    { y += 0.05f; }
      if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))  { y -= 0.05f; }
      if (Keyboard.isKeyDown(Keyboard.KEY_W))     { z -= 0.05f; }
      if (Keyboard.isKeyDown(Keyboard.KEY_S))     { z += 0.05f; }

      x += s;
      if (Math.abs(x) > 20) {
        s *= -1;
      }

      // Prepare rendering
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glClearColor(0, 0, 0, 1);

      // Draw stuff
      mesh.render(x, y, z); // room 1
      mesh.render(x - 13, y, z); // room 2
      mesh.render(x + 13, y, z); // room 3
      mesh.render(x, y - 10, z); // room 4
      mesh.render(x, y + 10, z); // room 5

      // Display sync
      Display.sync(Config.FPS_CAP);
      Display.update();
    }

    // Cleanup
    Shader.freeAll();
    GLObjects.freeAll();
    Display.destroy();
  }
}


// Useful constants to avoid hardcoding mystical values in the middle of even more mystical argument lists.
interface K {

  boolean debug = false;

  boolean no_transpose = false;

  int gl_null = 0;
  int offset0 = 0;
  int stride0 = 0;

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
    GL20.glVertexAttribPointer(attrId, attrSize, GL11.GL_FLOAT, false, K.stride0, K.offset0);
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

final class VaoAttrDescr {
  final int positions;
  final int uvs;
  VaoAttrDescr(int p, int u) {
    positions = p;
    uvs = u;
  }

  static final VaoAttrDescr base_descr = new VaoAttrDescr(K.attr0, K.attr1);
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

  float PROJECTION_FOV  = 45;
  float PROJECTION_NEAR = 0.1f;
  float PROJECTION_FAR  = 50f;
}


// Geometry data
interface Data {
  float[] vertices = {
    // Ground points
    -6f,      4f,       0f,       // v0: top left
    6f,       4f,       0f,       // v1: top right
    6f,       -4f,      0f,       // v2: bot right
    -6f,      -4f,      0f,       // v3: bot left
    // Top wall
    -6f,      4f,       3f,
    6f,       4f,       3f,
    6f,       4f,       0f,
    -6f,      4f,       0f,
    // Right wall
    6f,       4f,       3f,
    6f,       -4f,      3f,
    6f,       -4f,      0f,
    6f,       4f,       0f,
    // Bottom wall
    6f,       -4f,       3f,
    -6f,      -4f,       3f,
    -6f,      -4f,       0f,
    6f,       -4f,       0f,
    // Left wall
    -6f,      -4f,      3f,
    -6f,      4f,       3f,
    -6f,      4f,       0f,
    -6f,      -4f,      0f,
  };

  int[] indices = {
    // Ground
    0, 3, 1,  // upper left triangle
    1, 3, 2,  // lower right triangle
    // Top wall
    4, 7, 5,
    5, 7, 6,
    // Right wall
    8, 11, 9,
    9, 11, 10,
    // Bottom wall
    12, 15, 13,
    13, 15, 14,
    // Left wall
    16, 19, 17,
    17, 19, 18,
  };

  float[] tex_uvs = { // same orders as vertices
    // Ground
    0.0f,    0.0f,
    12.0f,   0.0f,
    12.0f,   8.0f,
    0.0f,    8.0f,
    // Top wall
    0.0f,    0.0f,
    6.0f,   0.0f,
    6.0f,   3.0f,
    0.0f,    3.0f,
    //  wall
    0.0f,    0.0f,
    4.0f,    0.0f,
    4.0f,    3.0f,
    0.0f,    3.0f,
    // Top wall
    0.0f,    0.0f,
    6.0f,   0.0f,
    6.0f,   3.0f,
    0.0f,    3.0f,
    // Top wall
    0.0f,    0.0f,
    4.0f,    0.0f,
    4.0f,    3.0f,
    0.0f,    3.0f,
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
  //static final class Example {
  //  static final Shader s = Shader.make("Example", "attr1", "attr2");
  //  static final int loc_var1 = Shader.locationOf(s, "var1");
  //  static void loadVar1(float dx, float dy, float dz) {
  //    Shader.load3f(loc_var1, dx, dy, dz);
  //  }
  //}
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

  static int[] testPixels() {
    int turquoise_light = PixelUtil.rgba(72, 216, 255, 0xff);
    int turquoise_base = PixelUtil.rgba(48, 144, 192, 0xff);
    int turquoise_dark = PixelUtil.rgba(0x20, 0x60, 0x80, 0xff);

    int w = 16;
    int h = 16;
    int l = w * h;
    int[] pixels = new int[l];

    for (int i = 0; i < l; i++) {
      pixels[i] = turquoise_base;
    }
    for (int i = 0; i < 16; i++) {
      pixels[i * 16] = turquoise_light;
      pixels[i] = turquoise_light;
      pixels[15 * 16 + i] = turquoise_dark;
      pixels[15 + 16 * i] = turquoise_dark;
    }
    pixels[0] = turquoise_base;
    pixels[l-1] = turquoise_base;
    pixels[15] = turquoise_base;
    pixels[l-16] = turquoise_base;
    return pixels;
  }

  static final Texture test_texture = Texture.create(16, 16, Texture.testPixels());
}

final class VecUtil {

  static void translationMatrix(Matrix4f out, Vector3f trans, float scale) {
    out.setIdentity();
    Matrix4f.translate(trans, out, out);
    Matrix4f.scale(new Vector3f(scale, scale, scale), out, out);
  }

  static void transformationMatrix(Matrix4f out, Vector3f trans, float rx, float ry, float rz, float scale) {
    out.setIdentity();
    Matrix4f.translate(trans, out, out);
    Matrix4f.rotate((float) Math.toRadians(rx), new Vector3f(1, 0, 0), out, out);
    Matrix4f.rotate((float) Math.toRadians(ry), new Vector3f(0, 1, 0), out, out);
    Matrix4f.rotate((float) Math.toRadians(rz), new Vector3f(0, 0, 1), out, out);
    Matrix4f.scale(new Vector3f(scale, scale, scale), out, out);
  }

  static Matrix4f projectionMatrix(float fov, float near, float far) {
    float w = Config.WIDTH;
    float h = Config.HEIGHT;
    float a = w / h;
    float len = far - near;
    float scale = (float) (1.0f / Math.tan(Math.toRadians(fov / 2)));

    Matrix4f proj = new Matrix4f();
    proj.m00 = scale / a;
    proj.m11 = scale;
    proj.m22 = - (near + far) / len;
    proj.m33 = 0;
    proj.m23 = -1;
    proj.m32 = - 2 * near * far / len;
    if (K.debug) System.out.println(proj);
    return proj;
  }
}

final class Mesh {
  static final int ATTR_POS = K.attr0;
  static final int ATTR_UVS = K.attr1;

  static final Shader shader        = Shader.make("static_room", "position", "uv");
  static final int loc_translation  = Shader.locationOf(shader, "translation");
  static final int loc_projection   = Shader.locationOf(shader, "projection");

  static {
      Shader.use(shader);
      Shader.load3f(loc_translation, 0, 0, 0);
      Shader.loadMat4f(loc_projection, Game.proj);
      Shader.stop();
  }

  int vaoId;
  int vertexCount;
  int textureId = Texture.test_texture.texId;

  void render(float dx, float dy, float dz) {
    Shader.use(shader);
    Shader.load3f(loc_translation, dx, dy, dz);
    GLUtil.vaoBind(vaoId);
    GLUtil.vertexAttribArrayBind(ATTR_POS);
    GLUtil.vertexAttribArrayBind(ATTR_UVS);
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GLUtil.textureBind(textureId);
    GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, K.offset0);
    GLUtil.vertexAttribArrayUnbind(ATTR_POS);
    GLUtil.vertexAttribArrayUnbind(ATTR_UVS);
    GLUtil.vaoUnbind();
    Shader.stop();
  }

  static Mesh load(float[] positions, int[] indices, float[] uvs) {
    int vaoId = GLObjects.allocVao();
    GLUtil.vaoBind(vaoId);
    GLUtil.bindIndices(indices);
    GLUtil.attributeStore(ATTR_POS, K.float_per_vertex, positions);
    GLUtil.attributeStore(ATTR_UVS, K.float_per_uv, uvs);
    GLUtil.vaoUnbind();
    Mesh m = new Mesh();
    m.vaoId = vaoId;
    m.vertexCount = indices.length;
    return m;
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


