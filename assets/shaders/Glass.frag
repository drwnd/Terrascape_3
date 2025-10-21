#version 400 core

flat in int textureData;
in vec3 totalPosition;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out int side;

uniform sampler2DArray textures;

vec2 getUVOffset(int side) {
    switch (side) {
        case 0: return vec2(fract(totalPosition.x * 0.0625), 1 - fract(totalPosition.y * 0.0625)) * 0.0625;
        case 1: return fract(totalPosition.xz * 0.0625) * 0.0625;
        case 2: return 0.0625 - fract(totalPosition.zy * 0.0625) * 0.0625;
        case 3: return 0.0625 - fract(totalPosition.xy * 0.0625) * 0.0625;
        case 4: return fract(totalPosition.zx * 0.0625) * 0.0625;
        case 5: return vec2(fract(totalPosition.z * 0.0625), 1 - fract(totalPosition.y * 0.0625)) * 0.0625;
    }

    return fract(totalPosition.zx);
}

void main() {
    side = textureData >> 8 & 7;
    vec4 color = texture(textures, vec3(getUVOffset(side), textureData & 0xFF));
    if (color.a == 0) discard;

    fragColor = color;
}
