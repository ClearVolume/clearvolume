#version 150
 
 /**
 * Fragment shader for rendering layer textures.
 *
 * @author Loic Royer 2014
 *
 */
 
// IMPORTANT NOTE: do not remove the 'insertpoint' comments, this is used to automatically generate variants of this shader  
 
uniform sampler2D texUnit0; 
//insertpoint1
 
in vec2 ftexcoord;

out vec4 outColor;
 

 
void main()
{
    vec4 tempOutColor = texture(texUnit0, ftexcoord);
    //insertpoint2
    
    outColor = tempOutColor;
}
