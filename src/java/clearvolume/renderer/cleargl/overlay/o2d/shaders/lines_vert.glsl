#version 330 core
 
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec3 vertexNormal;
layout(location = 2) in vec2 vertexTexCoord;


uniform mat4 projection;


void main()
{

   gl_Position = projection*vec4(vertexPosition, 1);
   
  
}



