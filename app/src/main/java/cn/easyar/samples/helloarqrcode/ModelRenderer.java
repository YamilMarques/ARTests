package cn.easyar.samples.helloarqrcode;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import cn.easyar.Matrix44F;
import cn.easyar.Vec2F;

public class ModelRenderer {

    private List<String> verticesList;
    private List<String> facesList;

    /*Buffer - Comm with OpenGL*/
    private FloatBuffer verticesBuffer;
    private ShortBuffer facesBuffer;

    /*Program
    * Vertices and Faces are not directly used to rendering, we use program GLES to do so*/
    private int program;


    public ModelRenderer(Context context){
        verticesList = new ArrayList<>();
        facesList = new ArrayList<>();

        try {

            /*Vertices and Faces setup*/
            verticesAndFacesSetup(context);

            /*Compile shaders*/
            compileShaders(context);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verticesAndFacesSetup(Context context) throws IOException {
        Scanner scanner = new Scanner(context.getAssets().open("model.obj"));
        while (scanner.hasNextLine()){
            String line = scanner.nextLine();
            if (line.startsWith("v ")){
                /*Vertex*/
                verticesList.add(line);
            }else {
                if (line.startsWith("f ")){
                    /*Face*/
                    facesList.add(line);
                }
            }
        }
        scanner.close();

        // Create buffer for vertices
        ByteBuffer buffer1 = ByteBuffer.allocateDirect(verticesList.size() * 3 * 4); /*Four bytes for each coordinate*/
        buffer1.order(ByteOrder.nativeOrder());
        verticesBuffer = buffer1.asFloatBuffer();

        // Create buffer for faces
        ByteBuffer buffer2 = ByteBuffer.allocateDirect(facesList.size() * 3 * 2); /*Four bytes for each coordinate*/
        buffer2.order(ByteOrder.nativeOrder());
        facesBuffer = buffer2.asShortBuffer();

        /*Populating vertices buffer*/
        for(String vertex: verticesList) {
            String coords[] = vertex.split(" "); /*Split by space*/
            float x = Float.parseFloat(coords[1]);
            float y = Float.parseFloat(coords[2]);
            float z = Float.parseFloat(coords[3]);
            verticesBuffer.put(x);
            verticesBuffer.put(y);
            verticesBuffer.put(z);
        }
        verticesBuffer.position(0); /*Reset the buffer position*/

        /*Populating faces buffer*/
        for(String face: facesList) {
            String vertexIndices[] = face.split(" ");
            short vertex1 = Short.parseShort(vertexIndices[1]);
            short vertex2 = Short.parseShort(vertexIndices[2]);
            short vertex3 = Short.parseShort(vertexIndices[3]);
            facesBuffer.put((short)(vertex1 - 1)); /*Faces indices start from 1 , so we substract one*/
            facesBuffer.put((short)(vertex2 - 1));
            facesBuffer.put((short)(vertex3 - 1));
        }
        facesBuffer.position(0);
    }

    private void compileShaders(Context context) throws IOException {
        // Convert vertex_shader.txt to a string
        InputStream vertexShaderStream = context.getResources().openRawResource(R.raw.vertex_shader);
        String vertexShaderCode = IOUtils.toString(vertexShaderStream, Charset.defaultCharset());
        vertexShaderStream.close();

        // Convert fragment_shader.txt to a string
        InputStream fragmentShaderStream = context.getResources().openRawResource(R.raw.fragment_shader);
        String fragmentShaderCode = IOUtils.toString(fragmentShaderStream, Charset.defaultCharset());
        fragmentShaderStream.close();

        /* Adding shaders code to GLES */
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderCode);

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);

        /* Pass data to gl compiler */
        GLES20.glCompileShader(vertexShader);
        GLES20.glCompileShader(fragmentShader);

        /* Program generation */
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        /*Linking program in order to be able to use it*/
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program);
    }

    public void render(Matrix44F projectionMatrix, Matrix44F cameraview, Vec2F size){

        int position = GLES20.glGetAttribLocation(program, "position");
        GLES20.glEnableVertexAttribArray(position);

        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 3 * 4, verticesBuffer);

        /*GLES20.glUniformMatrix4fv(pos_trans_box, 1, false, cameraview.data, 0);
        GLES20.glUniformMatrix4fv(pos_proj_box, 1, false, projectionMatrix.data, 0);*/


       /* Matrix.frustumM(projectionMatrix.data, 0,
                -1, 1,
                -1, 1,
                2, 9);*/

       /* Matrix.setLookAtM(cameraview.data, 0,
                0, 3, -4,
                0, 0, 0,
                0, 1, 0);*/


        float[] productMatrix = new float[16];
        Matrix.multiplyMM(productMatrix, 0, projectionMatrix.data, 0, cameraview.data, 0);

        int matrix = GLES20.glGetUniformLocation(program, "matrix");
        GLES20.glUniformMatrix4fv(matrix, 1, false, productMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, facesList.size() * 3, GLES20.GL_UNSIGNED_SHORT, facesBuffer);

        GLES20.glDisableVertexAttribArray(position);
    }


}
