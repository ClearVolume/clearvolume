#version 400 core

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

uniform vec4 color; 
out vec4 outColor;
in float attenuation; 
 
void main()
{
  outColor = vec4(0.0, 1.0, 0.0, attenuation);
}
