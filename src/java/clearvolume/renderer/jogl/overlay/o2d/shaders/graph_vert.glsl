#version 150
 
uniform mat4 projection;

in vec4 position;

void main()
{
   gl_Position =  projection * position;   
}


