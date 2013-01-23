uniform mat4 u_MVPMatrix;	// A constant representing the combined model/view/projection matrix.
uniform mat4 u_MVMatrix;	// A constant representing the combined model/view matrix.	
uniform mediump vec4 u_Color;		// Object color information we will pass in.
uniform bool u_Lighting;

attribute vec4 a_Position;	// input. Vertex position in model coordinates
attribute vec3 a_Normal;	// input. Vertex normal   in model coordinates

varying vec4 v_Color;		// This will be passed into the fragment shader.

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

void main() {				// The entry point for our vertex shader.
	// gl_Position is a special variable used to store the final position.
	// Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
	gl_Position = u_MVPMatrix * a_Position;

	if (u_Lighting) {
		// Transform the vertex into eye space.
		vertexPosition = vec3(u_MVMatrix * a_Position);
		// Transform the normal's orientation into eye space.
		vertexNormal = normalize(vec3(u_MVMatrix * vec4(a_Normal, 0.0)));
	
		v_Color = u_Color * ambientColor
				+ lightVertex(light0Position, light0DiffuseColor)
				+ lightVertex(light1Position, light1DiffuseColor);
		v_Color = clamp(v_Color, 0.0, 1.0);
	
		float fog = exp(-0.005 * gl_Position.z);
		v_Color = fog * v_Color + (1.0-fog) * fogColor;
	} else {
		v_Color = u_Color;
	}
}