#version 330 core

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

uniform vec2  linepos 		; 
uniform float linelength	;
uniform float linethick		;
uniform float lineperiod	;
uniform float boxlinesalpha	;
uniform float alpha			;

uniform vec4  color 		=	vec4(1, 1, 1, 1.0);
out vec4 outColor;

 
void main()
{
    vec2 distgrid = fract((VertexIn.TexCoord-linepos)*lineperiod+vec2(0.5,0.5)) -vec2(0.5,0.5);
	
	
	float alphar = 1/(1+linelength*(abs(distgrid.x)+abs(distgrid.y)));
	float alphax = 1/(1+linethick*(abs(distgrid.x)));
	float alphay = 1/(1+linethick*(abs(distgrid.y)));
	
	vec2 distmainbox = fract(VertexIn.TexCoord+vec2(0.5,0.5)) -vec2(0.5,0.5);
	float intx = 1/(1+400*(abs(distmainbox.x)));
	float inty = 1/(1+400*(abs(distmainbox.y)));

	float intensity = alpha*(alphar*(alphax+alphay)+boxlinesalpha*(intx+inty)); //intx + inty;

    outColor = vec4(color.r*intensity,color.g*intensity,color.b*intensity, color.a);
    
}


