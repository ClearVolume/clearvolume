#version 150
 
uniform vec4 color; 
 

out vec4 outColor;
 
void main()
{
  outColor = color;
//outColor = vec4(1.,0,0,1.);
}
