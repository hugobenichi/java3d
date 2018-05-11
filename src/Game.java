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
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.PixelFormat;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


// The main class
public class Game {

  public static void main(String[] args) throws Exception {


    // DISPLAY MANAGEMENT
    Display.setDisplayMode(new DisplayMode(Config.WIDTH, Config.HEIGHT));
    Display.create(new PixelFormat(), new ContextAttribs(3, 2).withForwardCompatible(true).withProfileCore(true));
    Display.setTitle(Config.TITLE);

    GL11.glViewport(0, 0, Config.WIDTH, Config.HEIGHT);


    // Shader loading
    Shader s = Shader.tex;
    //Shader s = Shader.gradient;

    // Texture loading
    Texture tex = Texture.test_texture;

    // Geometry startup
    // TODO: Model should store the texture, and maybe the shader too.
    Model model = modelMake(Data.vertices, Data.indices, Data.tex_uvs);


    while (!Display.isCloseRequested()) {
      // TODO: get input
      // TODO: run game logic

      // Prepare rendering
      GL11.glClearColor(1, 0, 1, 1); // RGBA
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

      // Render models
      // TODO: put shaders and models together
      Shader.use(s);
      modelRender(model, tex);
      Shader.stop();

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

  static void modelRender(Model model, Texture tex) {
    GLUtil.vaoBind(model.vaoId);
    GLUtil.vertexAttribArrayBind(K.attr0); // CLEANUP: hardcoded ! put this into the model instead
    GLUtil.vertexAttribArrayBind(K.attr1);
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GLUtil.textureBind(tex.texId);
    GL11.glDrawElements(GL11.GL_TRIANGLES, model.vertexCount, GL11.GL_UNSIGNED_INT, K.offset0);
    GLUtil.vertexAttribArrayUnbind(K.attr0);
    GLUtil.vertexAttribArrayUnbind(K.attr1);
    GLUtil.vaoUnbind();
  }
}


// Useful constants to avoid hardcoding mystical values in the middle of even more mystical argument lists.
interface K {

  int gl_null = 0;
  int offset0 = 0;

  int attr0 = 0;
  int attr1 = 1;

  int float_per_vertex = 3;
  int float_per_uv = 2;
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
class GLObjects {
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


class IOUtil {
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
  String TITLE        = "Our First Display";
}


// Geometry data
interface Data {
  float[] vertices = {
    -0.5f,      0.5f,       0f,       // top left
    -0.5f,      -0.5f,      0f,       // bot left
    0.5f,       -0.5f,      0f,       // bot right
    0.5f,       0.5f,       0f,       // top right
  };

  int[] indices = {
    0, 1, 3,
    3, 1, 2,
  };

  float[] tex_uvs = { // same orders as vertices
    0.0f,    0.0f,
    0.0f,    1.0f,
    1.0f,    1.0f,
    1.0f,    0.0f,
  };
}


// Used to track Vertex Array Object data.
class Model {
  int vaoId;
  int vertexCount;
}


class Shader {
  int programId;
  int vertexId;
  int fragmentId;
  String[] bindings;

  static final String SKIP = "SKIP_BINDING";

  static Shader make(String shaderdir, String... bindings) {
    Shader s = new Shader();
    s.vertexId = loadShader(shaderdir + "/vertex", GL20.GL_VERTEX_SHADER);
    s.fragmentId = loadShader(shaderdir + "/fragment", GL20.GL_FRAGMENT_SHADER);
    s.programId = GL20.glCreateProgram();
    s.bindings = bindings;
    GL20.glAttachShader(s.programId, s.vertexId);
    GL20.glAttachShader(s.programId, s.fragmentId);
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

  void free() {
    GL20.glDetachShader(programId, vertexId);
    GL20.glDetachShader(programId, fragmentId);
    GL20.glDeleteShader(vertexId);
    GL20.glDeleteShader(fragmentId);
    GL20.glDeleteShader(programId);
  }

  static void use(Shader s) {
    GL20.glUseProgram(s.programId);
    for (int i = 0; i < s.bindings.length; i++) {
      if (s.bindings[i] == SKIP) {
        continue;
      }
      GL20.glBindAttribLocation(s.programId, i, s.bindings[i]);
    }
  }

  static void stop() {
    GL20.glUseProgram(0);
  }

  // Statically load all shaders
  static final List<Shader> shaders = new ArrayList<>();

  static final Shader gradient = Shader.make(shaderloc("gradient"));
  static final Shader tex = Shader.make(shaderloc("tex"), "position", "uv");

  static String shaderloc(String leafdir) {
    return "./src/shaders/" + leafdir;
  }

  static void freeAll() {
    shaders.forEach(Shader::free);
  }
}


class PixelUtil {
  // Getters
  static int a(int rgba) { return 0xff & (rgba >> 24); }
  static int r(int rgba) { return 0xff & (rgba >> 16); }
  static int g(int rgba) { return 0xff & (rgba >>  8); }
  static int b(int rgba) { return 0xff & rgba; }

  // TODO: Setters
}


class Texture {

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
    //GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
    //GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    //GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

    return t;
  }

  static int[] testPixels() {
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
