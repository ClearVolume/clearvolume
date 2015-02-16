#version 410 core

layout(lines) in;
layout(triangle_strip, max_vertices=4) out;

in VertexData {
    vec3 Position;
    vec3 Normal;
    vec2 TexCoord;
} VertexIn[];

uniform float EdgeWidth = 0.05;
uniform float PctExtend = 0.05;

/*bool isFrontFacing(vec3 a, vec3 b, vec3 c) {
    return(( a.x * b.y - b.x * a.y ) +
           ( b.x * c.y - c.x * b.y ) +
           ( c.x * a.y - c.y * a.x )) > 0;
}*/

out vec4 color;

void emitEdgeQuad(vec3 e0, vec3 e1) {
    vec2 ext = PctExtend * (e1.xy - e0.xy);
    vec2 v = normalize(e1.xy - e0.xy);
    vec2 n = vec2(-v.y, v.x) * EdgeWidth;

    gl_Position = vec4( e0.xy - ext, e0.z, 1.0 );
    EmitVertex();

    gl_Position = vec4( e0.xy - n - ext, e0.z, 1.0 );
    EmitVertex();

    gl_Position = vec4( e1.xy + ext, e1.z, 1.0);
    EmitVertex();

    gl_Position = vec4( e1.xy - n + ext, e1.z, 1.0);
    EmitVertex();

    EndPrimitive();
}

void main() {
    vec3 p0 = gl_in[0].gl_Position.xyz / gl_in[0].gl_Position.w;
    vec3 p1 = gl_in[1].gl_Position.xyz / gl_in[1].gl_Position.w;
    emitEdgeQuad(p0, p1);

    color = vec4(1.0, 0.0, 0.0, 1.0);
}