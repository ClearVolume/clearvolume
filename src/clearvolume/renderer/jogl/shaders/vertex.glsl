#version 150
 
in vec4 position;
in vec2 texcoord;
 
out vec2 ftexcoord;
 
void main()
{
    ftexcoord = texcoord;
    gl_Position = position;
}


