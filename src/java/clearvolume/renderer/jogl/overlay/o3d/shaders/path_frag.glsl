#version 400 core

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

in vec4 color;
out vec4 outColor;

 
void main()
{
    outColor = color;
}