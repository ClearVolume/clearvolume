#version 330 core

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

uniform vec4 color = vec4(1.0, 0.0, 0.0, 1.0);
out vec4 outColor;

 
void main()
{
    vec2 distgrid = fract(VertexIn.TexCoord*10.0+vec2(0.5,0.5)) -vec2(0.5,0.5);
	
	float alphax = 1/(1+100*(abs(distgrid.x)));
	float alphay = 1/(1+100*(abs(distgrid.y)));
	
	vec2 distmainbox = fract(VertexIn.TexCoord+vec2(0.5,0.5)) -vec2(0.5,0.5);
	float intx = 1/(1+400*(abs(distmainbox.x)));
	float inty = 1/(1+400*(abs(distmainbox.y)));

	float intensity = 0.3*(alphax+alphay+intx+inty); //intx + inty;

    outColor = vec4(intensity,intensity,intensity, 1);
    
}


//bvec2 toDiscard = greaterThan(fract(VertexIn.TexCoord*10.0), vec2(0.01, 0.01));
//
//    if(all(toDiscard)) {
//        discard;
//    } else {
//        outColor = vec4(0.5, 0.5, 0.5, 1.0);
//    }
