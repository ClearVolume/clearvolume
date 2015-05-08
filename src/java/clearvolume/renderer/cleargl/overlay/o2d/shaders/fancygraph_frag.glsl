#version 330 core

in VertexData {
    vec4 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

uniform vec4 color = vec4(.2, 0.6, 1., 1.0);
out vec4 outColor;

 
void main()
{

	float intensity = exp(-1.5f*(1.f-VertexIn.TexCoord.y));
    
    
    
    outColor = intensity*color;
    outColor.w = .75f;
}


//bvec2 toDiscard = greaterThan(fract(VertexIn.TexCoord*10.0), vec2(0.01, 0.01));
//
//    if(all(toDiscard)) {
//        discard;
//    } else {
//        outColor = vec4(0.5, 0.5, 0.5, 1.0);
//    }