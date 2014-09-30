#version 150
 
uniform sampler2D texUnit; 
 
in vec2 ftexcoord;

out vec4 outColor;
 
void main()
{
    outColor = texture(texUnit, ftexcoord);
}
