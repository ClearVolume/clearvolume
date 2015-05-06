#version 330 core
 
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec3 vertexNormal;
layout(location = 2) in vec2 vertexTexCoord;

out VertexData {
    vec4 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexOut;

uniform mat4 projection;


void main()
{
   VertexOut.Normal = vertexNormal;
   VertexOut.Position = projection*vec4(vertexPosition,1);
   VertexOut.TexCoord = vertexTexCoord;

   gl_Position = projection*vec4(vertexPosition, 1);
}



