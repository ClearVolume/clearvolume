#version 330 core


uniform vec4 color = vec4(1.0, 0.0, 0.0, 1.0);
out vec4 outColor;


in vec2 texCoord;

in float value;
in float strip_thickness;
in float strip_len;


void main()
{

	float border_dist_r = 1.f/strip_thickness*min(length(texCoord),length(texCoord-vec2(strip_len,0.f)));
	
	float border_dist_y = 1.f/strip_thickness*abs(texCoord.y);
	
	
	float border_dist = ((texCoord.x<-0.f) || (texCoord.x>strip_len))?border_dist_r:border_dist_y;
	
	
	//float border_dist_x = min(abs(texCoord.x+strip_thickness),abs(texCoord.x-strip_len-strip_thickness));
    //float border_dist_y = min(abs(texCoord.y+strip_thickness),abs(texCoord.y-strip_thickness));
     
	float intensity = max(1.5*exp(-5.f*border_dist_r),1.1*exp(-5.f*border_dist));
	
	
	//intensity = border_dist_y;
    outColor = vec4(intensity*0.2,intensity*0.6,intensity, 1.);
    
}
