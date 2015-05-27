#version 330 core
 
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec3 vertexNormal;
layout(location = 2) in vec2 vertexTexCoord;

out VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexOut;

uniform mat4 modelview;
uniform mat4 projection;


void main()
{
   VertexOut.Normal = vertexNormal;
   VertexOut.Position = vec3(modelview*vec4(vertexPosition, 1.0));
   VertexOut.TexCoord = vertexTexCoord;

   gl_Position = (projection*modelview)*vec4(vertexPosition , 1.0); 
}


