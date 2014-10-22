#version 150
 
in vec3 position;
uniform mat4 modelview;
uniform mat4 projection;
  
void main()
{
 
  // gl_Position = projection * modelview * vec4(.5*position.xyz,1.0f);
   gl_Position =  projection * modelview* vec4(2*position.xyz,1.0f);
   
    //gl_Position = vec4(position.xyz,1.0f);
}


