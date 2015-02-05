#version 400 core
 
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

out float attenuation;  

uniform vec3 position = vec3(0.0, 0.0, 0.0);

void main()
{
   VertexOut.Normal = vertexNormal;
   VertexOut.Position = vec3(modelview*vec4(vertexPosition, 1.0));
   VertexOut.TexCoord = vertexTexCoord;
   attenuation = 1;

   gl_Position = modelview*projection*vec4(vertexPosition + position, 1.0);
}


