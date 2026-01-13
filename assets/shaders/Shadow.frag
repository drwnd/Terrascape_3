#version 400 core
#define SHADOW_TRANSPARENT_ORDINAL 4
#define PROPERTIES_OFFSET 24

flat in int textureData;

void main() {
    if ((textureData & 1 << PROPERTIES_OFFSET + SHADOW_TRANSPARENT_ORDINAL) != 0) discard;
}