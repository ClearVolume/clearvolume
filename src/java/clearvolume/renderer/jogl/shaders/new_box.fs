#version 400 core

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn;

uniform vec4 color = vec4(1.0, 0.0, 0.0, 1.0);
out vec4 outColor;
in float attenuation; 
 
void main()
{
    bvec2 toDiscard = greaterThan(fract(VertexIn.TexCoord*10.0), vec2(0.015, 0.015));

    if(all(toDiscard)) {
        discard;
    } else {
        outColor = vec4(0.5, 0.5, 0.5, 1.0);
    }
}
