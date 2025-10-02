#version 400 core

flat in int textureData;
flat in vec3 normal;
in vec3 totalPosition;

out vec4 fragColor;

//layout(location = 0) out vec3 fragNormal;
//layout(location = 1) out vec3 fragPosition;
//layout(location = 2) out vec4 fragColor;
//layout(location = 3) out float fragProperties;

uniform sampler2DArray textures;
uniform sampler2DArray propertiesTextures;

vec2 getUVOffset(int side) {
    switch (side) {
        case 0: return vec2(fract(totalPosition.x * 0.0625), 1 - fract(totalPosition.y * 0.0625));
        case 1: return fract(totalPosition.xz * 0.0625);
        case 2: return 1 - fract(totalPosition.zy * 0.0625);
        case 3: return 1 - fract(totalPosition.xy * 0.0625);
        case 4: return fract(totalPosition.xz * 0.0625);
        case 5: return vec2(fract(totalPosition.z * 0.0625), 1 - fract(totalPosition.y * 0.0625));
    }

    return fract(totalPosition.zx);
}

void main() {
    vec3 textureCoord = vec3(getUVOffset(textureData >> 8 & 7), textureData & 0xFF);
    vec4 color = texture(textures, textureCoord);
    float fragProperties = texture(propertiesTextures, textureCoord).r;

    if (color.a == 0) discard;

    vec3 fragPosition = totalPosition;
    vec3 fragNormal = normal;

    fragColor = vec4(color);
}
