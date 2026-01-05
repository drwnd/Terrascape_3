#version 460 core

flat out int instanceID;

struct AABB {
    int x, y, z;
    int packedSize;
};

layout (std430, binding = 0) restrict readonly buffer AABBsBuffer {
    AABB[] aabbs;
};

uniform mat4 projectionViewMatrix;
uniform ivec3 iCameraPosition;

const ivec2[6] FACE_POSITIONS = ivec2[6](ivec2(0, 0), ivec2(0, 1), ivec2(1, 0), ivec2(1, 1), ivec2(1, 0), ivec2(0, 1));
const int NORTH = 0;
const int TOP = 1;
const int WEST = 2;
const int SOUTH = 3;
const int BOTTOM = 4;
const int EAST = 5;

ivec3 getFacePositions(int side, int currentVertexId, ivec3 minPosition, ivec3 maxPosition) {
    ivec3 currentVertexOffset = ivec3(FACE_POSITIONS[currentVertexId].xy, 0);

    switch (side) {
        case NORTH: return (1 - currentVertexOffset.yxz) * maxPosition + currentVertexOffset.yxz * minPosition;
        case TOP: return (1 - currentVertexOffset.xzy) * maxPosition + currentVertexOffset.xzy * minPosition;
        case WEST: return (1 - currentVertexOffset.zyx) * maxPosition + currentVertexOffset.zyx * minPosition;
        case SOUTH: return currentVertexOffset.xyz * maxPosition + (1 - currentVertexOffset.xyz) * minPosition;
        case BOTTOM: return currentVertexOffset.yzx * maxPosition + (1 - currentVertexOffset.yzx) * minPosition;
        case EAST: return currentVertexOffset.zxy * maxPosition + (1 - currentVertexOffset.zxy) * minPosition;
    }

    return ivec3(0, 0, 0);
}

void main() {
    AABB currentAABB = aabbs[gl_InstanceID];
    int side = gl_VertexID / 6 % 6;
    int currentVertexId = gl_VertexID % 6;

    int lod = currentAABB.packedSize >> 21;
    int lengthX = currentAABB.packedSize >> 14 & 0x7F;
    int lengthY = currentAABB.packedSize >> 7 & 0x7F;
    int lengthZ = currentAABB.packedSize & 0x7F;

    ivec3 minPosition = ivec3(currentAABB.x, currentAABB.y, currentAABB.z);
    ivec3 maxPosition = minPosition + ivec3(lengthX << lod, lengthY << lod, lengthZ << lod);

    ivec3 totalPosition = getFacePositions(side, currentVertexId, minPosition, maxPosition) - iCameraPosition;
    gl_Position = projectionViewMatrix * vec4(totalPosition, 1.0);

    instanceID = gl_InstanceID;
}