#version 330 core

layout (lines) in;
layout (triangle_strip, max_vertices = 5) out;

float thickness = .04f;


out vec2 texCoord;
out float value;

out float strip_thickness;
out float strip_len;

void main(void)
{
    vec4 dist = vec4(gl_in[1].gl_Position.xy-gl_in[0].gl_Position.xy,0.f,0.f);
    float dist_len = length(dist);
    dist = thickness*normalize(dist);
    vec4 normal = vec4(-dist.y,dist.x,0.f,0.f);
    
    
    
	gl_Position = gl_in[0].gl_Position-normal-dist;
	texCoord = vec2(-thickness,-thickness);
	strip_thickness = thickness;
	strip_len = dist_len;
    EmitVertex();
    
    
    gl_Position = gl_in[0].gl_Position+normal-dist;
    texCoord = vec2(-thickness,thickness);
    strip_thickness = thickness;
    strip_len = dist_len;
    EmitVertex();
    
    gl_Position = gl_in[1].gl_Position-normal+dist;
    texCoord = vec2(dist_len+thickness,-thickness);
    strip_thickness = thickness;
    strip_len = dist_len;
    EmitVertex();

    
    gl_Position = gl_in[1].gl_Position+normal+dist;
    texCoord = vec2(dist_len+thickness,thickness);
    strip_thickness = thickness;
    strip_len = dist_len;
    EmitVertex();
    
   
    EndPrimitive();
}

