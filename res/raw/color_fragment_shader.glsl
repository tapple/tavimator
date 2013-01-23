precision mediump float;       	// Set the default precision to medium. We don't need as high of a 
								// precision in the fragment shader.

uniform mediump vec4 u_Color;		// Object color information we will pass in.
uniform bool u_Lighting;

varying vec4 v_Color;          	// This is the color from the vertex shader interpolated across the 
  								// triangle per fragment.
  
varying vec3 vertexPosition;		// vertex position in eye coordinates
varying vec3 vertexNormal;			// vertex normal   in eye coordinates

const vec3 light0Position = vec3(0, 80, 100);
const vec4 light0DiffuseColor = vec4(1.0, 1.0, 1.0, 1.0);

const vec3 light1Position = vec3(0, 80, -100);
const vec4 light1DiffuseColor = vec4(0.5, 0.5, 0.5, 1.0);

const vec4  ambientColor = vec4(0.6, 0.6, 0.6, 1.0);
const vec4 specularColor = vec4(0.6, 0.6, 0.6, 0.0);
const vec4      fogColor = vec4(0.5, 0.5, 0.5, 0.3);

const float shininess = 100.0;

float pdot(const vec3 v1, const vec3 v2) {
	return max(dot(v1, v2), 0.0);
}

vec4 lightVertex(vec3 lightPosition, vec4 lightDiffuseColor) {
	vec3 lightDirection = normalize(lightPosition - vertexPosition);
	float diffuseFactor = pdot(vertexNormal, lightDirection);
	float specularFactor = 0.0;
	if (diffuseFactor != 0.0) {
		specularFactor = pow(pdot(vertexNormal, normalize(lightDirection + vec3(0,0,1))), shininess);
	}
	// Multiply the color by the illumination level. It will be interpolated across the triangle.
	return u_Color * lightDiffuseColor * diffuseFactor + specularColor * specularFactor;
}

// The entry point for our fragment shader.
void main()                    		
{                              	
	if (u_Lighting) {
		gl_FragColor = u_Color * ambientColor
				+ lightVertex(light0Position, light0DiffuseColor)
				+ lightVertex(light1Position, light1DiffuseColor);
		gl_FragColor = clamp(gl_FragColor, 0.0, 1.0);
	
		float fog = exp(-0.005 * gl_FragCoord.z / gl_FragCoord.w);
		gl_FragColor = fog * gl_FragColor + (1.0-fog) * fogColor;
	} else {
		gl_FragColor = u_Color;
	}
	// Pass through the color
    //gl_FragColor = v_Color;                                  		
}