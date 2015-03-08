#version 400 core

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

uniform vec2  linepos 		=	vec2(0.5,0.5); 
uniform float linelength	=	10;
uniform float linethick		=	100;
uniform float lineperiod	=	1;
uniform float boxlinesalpha	=	1;
uniform float alpha			=	1;

uniform vec4  color 		=	vec4(0.8, 0.8, 1, 1.0);
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


