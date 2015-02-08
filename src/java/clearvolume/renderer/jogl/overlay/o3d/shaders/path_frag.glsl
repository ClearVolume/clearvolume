#version 150
 
uniform vec4 color; 
out vec4 outColor;
in float attenuation; 
 
void main()
{
  outColor = vec4(color.xyz,attenuation);
}
