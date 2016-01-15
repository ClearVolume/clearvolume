#version 400

 /**
 * Fragment shader for rendering layer textures.
 *
 * @author Loic Royer & Ulrik Günther
 *
 */

// IMPORTANT NOTE: do not remove the 'insertpoint' comments, this is used to automatically generate variants of this shader

layout( location = 0) out vec4 FragColor;

//insertpoint1
uniform sampler2D texUnit0;

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

uniform mat4 ModelViewMatrix;
uniform mat4 Projection;
uniform mat4 MVP;
uniform int layer;

void main()
{
    vec4 tempOutColor = texture(texUnit0, VertexIn.TexCoord);
    //insertpoint2
    FragColor = tempOutColor;
}
