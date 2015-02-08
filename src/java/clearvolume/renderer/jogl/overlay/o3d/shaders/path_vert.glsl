#version 150
 
uniform mat4 modelview;
uniform mat4 projection;

in vec3 position;
  
out float attenuation;  

void main()
{
   gl_Position =  projection * modelview* vec4(2*position.xyz,1.0f);   
   attenuation = 1;
}


