extern crate jni;

use jni::objects::{JByteArray, JClass, JIntArray, JPrimitiveArray};
use jni::sys::{jint, jintArray};
use jni::{AttachGuard, Env, EnvUnowned};
use jni::elements::ReleaseMode;
use super::materials_data::{CHUNK_SIZE, CHUNK_SIZE_BITS, AIR, MaterialsData, NORTH, SOUTH, get_uncompressed_index, TOP, BOTTOM, WEST, EAST};

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_game_server_generation_NativeFunctions_generateMesh<'a>(
    env_unowned: EnvUnowned, _class: JClass,
    materials_data: JByteArray, surface_equivalent: JByteArray,
    north: JByteArray, top: JByteArray, west: JByteArray, south: JByteArray, bottom: JByteArray, east: JByteArray,
    x_start: jint, y_start: jint, z_start: jint) -> jintArray {
    let mut guard: AttachGuard = unsafe { AttachGuard::from_unowned(env_unowned.as_raw()) };
    let env: &mut Env = guard.borrow_env_mut();

    let mut auto_elements = unsafe { materials_data.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let materials_data: &mut [i8] = auto_elements.as_mut().unwrap().as_mut();
    let mut auto_elements = unsafe { surface_equivalent.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let surface_equivalent: &mut [i8] = auto_elements.as_mut().unwrap().as_mut();

    let mut auto_elements = unsafe { north.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let north: &[i8] = auto_elements.as_mut().unwrap().as_mut();
    let mut auto_elements = unsafe { top.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let top: &[i8] = auto_elements.as_mut().unwrap().as_mut();

    let mut auto_elements = unsafe { west.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let west: &[i8] = auto_elements.as_mut().unwrap().as_mut();
    let mut auto_elements = unsafe { south.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let south: &[i8] = auto_elements.as_mut().unwrap().as_mut();

    let mut auto_elements = unsafe { bottom.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let bottom: &[i8] = auto_elements.as_mut().unwrap().as_mut();
    let mut auto_elements = unsafe { east.get_elements_critical(&env, ReleaseMode::NoCopyBack) };
    let east: &[i8] = auto_elements.as_mut().unwrap().as_mut();

    let mesh_data: Vec<i32> = generate_mesh(materials_data, surface_equivalent, north, top, west, south, bottom, east, x_start, y_start, z_start);
    let result: JPrimitiveArray<jint> = JIntArray::new(env, mesh_data.len()).unwrap();

    let java_array = unsafe { result.get_elements_critical(&env, ReleaseMode::CopyBack) }.unwrap();
    unsafe { std::ptr::copy_nonoverlapping(mesh_data.as_ptr(), java_array.as_ptr(), mesh_data.len()); }

    result.as_raw() as jintArray
}

pub fn is_visible(to_test_material: i8, occluding_material: i8) -> bool {
    if to_test_material == AIR { return false; }
    if occluding_material == AIR { return true; }

    if (MATERIAL_PROPERTIES[occluding_material as u8 as usize] & TRANSPARENT) == 0 { return false; }
    if (MATERIAL_PROPERTIES[to_test_material as u8 as usize] & OCCLUDES_SELF_ONLY) == OCCLUDES_SELF_ONLY {
        return to_test_material != occluding_material;
    }

    true
}

fn generate_mesh(materials_data: &[i8], surface_equivalent: &[i8],
                 north: &[i8], top: &[i8], west: &[i8], south: &[i8], bottom: &[i8], east: &[i8],
                 x_start: i32, y_start: i32, z_start: i32) -> Vec<i32> {
    let mut generator: MeshGenerator = MeshGenerator::new(x_start, y_start, z_start);
    let mut to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6] = &mut [0; CHUNK_SIZE * CHUNK_SIZE * 6];

    let adjacent_chunk_layers: &[&[i8]; 6] = &[north, top, west, south, bottom, east];

    let materials: MaterialsData = MaterialsData { data: materials_data };
    let surface_equivalent: MaterialsData = MaterialsData { data: surface_equivalent };

    materials.fill_uncompressed_materials(&mut generator.uncompressed_materials, CHUNK_SIZE_BITS, 0, 0, 0, 0);
    surface_equivalent.generate_to_mesh_faces_maps(&mut to_mesh_faces_map, &mut generator.uncompressed_materials, adjacent_chunk_layers, CHUNK_SIZE_BITS, 0, 0, 0, 0);

    generator.generate_mesh(to_mesh_faces_map);
    generator.get_mesh_data()
}

struct MeshGenerator {
    uncompressed_materials: [i8; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE],
    materials_layer: [i8; CHUNK_SIZE * CHUNK_SIZE],
    vertices: [Vec<i32>; 9],
    x_start: i32,
    y_start: i32,
    z_start: i32,
}

impl MeshGenerator {
    fn new(x_start: i32, y_start: i32, z_start: i32) -> MeshGenerator {
        MeshGenerator {
            uncompressed_materials: [AIR; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE],
            materials_layer: [AIR; CHUNK_SIZE * CHUNK_SIZE],
            vertices: [Vec::new(), Vec::new(), Vec::new(), Vec::new(), Vec::new(), Vec::new(), Vec::new(), Vec::new(), Vec::new()],
            x_start,
            y_start,
            z_start,
        }
    }

    fn get_mesh_data(&mut self) -> Vec<i32> {
        let mut required_length: usize = 9;
        self.vertices.iter().for_each(|vec: &Vec<i32>| { required_length += vec.len(); });
        let mut mesh_data: Vec<i32> = Vec::with_capacity(required_length);

        self.vertices.iter().for_each(|vec: &Vec<i32>| { mesh_data.push((vec.len() * 3 / 4) as i32); });
        self.vertices.iter_mut().for_each(|vec: &mut Vec<i32>| { mesh_data.append(vec); });

        mesh_data
    }

    fn generate_mesh(&mut self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        self.add_north_south_faces(to_mesh_faces_map);
        self.add_top_bottom_faces(to_mesh_faces_map);
        self.add_west_east_faces(to_mesh_faces_map);
        if self.has_opaque_mesh() { self.add_side_layers() }
    }


    fn add_north_south_faces(&mut self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        for material_z in 0..CHUNK_SIZE {
            self.copy_materials_north_south(material_z, to_mesh_faces_map);
            self.add_north_south_layer(NORTH, material_z, to_mesh_faces_map);
            self.add_north_south_layer(SOUTH, material_z, to_mesh_faces_map);
        }
    }

    fn add_top_bottom_faces(&mut self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        for material_y in 0..CHUNK_SIZE {
            self.copy_materials_top_bottom(material_y, to_mesh_faces_map);
            self.add_top_bottom_layer(TOP, material_y, to_mesh_faces_map);
            self.add_top_bottom_layer(BOTTOM, material_y, to_mesh_faces_map);
        }
    }

    fn add_west_east_faces(&mut self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        for material_x in 0..CHUNK_SIZE {
            self.copy_materials_west_east(material_x, to_mesh_faces_map);
            self.add_west_east_layer(WEST, material_x, to_mesh_faces_map);
            self.add_west_east_layer(EAST, material_x, to_mesh_faces_map);
        }
    }


    fn copy_materials_north_south(&mut self, material_z: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_1: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_map, NORTH, material_z);
        let to_mesh_faces_2: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_map, SOUTH, material_z);

        for material_x in 0..CHUNK_SIZE {
            let mut required_materials: u64 = to_mesh_faces_1[material_x] | to_mesh_faces_2[material_x];
            let mut material_y: usize = required_materials.trailing_zeros() as usize;

            while material_y < CHUNK_SIZE {
                self.materials_layer[material_x << CHUNK_SIZE_BITS | material_y] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
                required_materials &= (-2i64 as u64) << material_y;
                material_y = required_materials.trailing_zeros() as usize;
            }
        }
    }

    fn copy_materials_top_bottom(&mut self, material_y: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_1: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_map, TOP, material_y);
        let to_mesh_faces_2: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_map, BOTTOM, material_y);

        for material_x in 0..CHUNK_SIZE {
            let mut required_materials: u64 = to_mesh_faces_1[material_x] | to_mesh_faces_2[material_x];
            let mut material_z: usize = required_materials.trailing_zeros() as usize;

            while material_z < CHUNK_SIZE {
                self.materials_layer[material_x << CHUNK_SIZE_BITS | material_z] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
                required_materials &= (-2i64 as u64) << material_z;
                material_z = required_materials.trailing_zeros() as usize;
            }
        }
    }

    fn copy_materials_west_east(&mut self, material_x: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_1: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_map, WEST, material_x);
        let to_mesh_faces_2: &[u64; CHUNK_SIZE] = MeshGenerator::split_map(to_mesh_faces_map, EAST, material_x);

        for material_z in 0..CHUNK_SIZE {
            let mut required_materials: u64 = to_mesh_faces_1[material_z] | to_mesh_faces_2[material_z];
            let mut material_y: usize = required_materials.trailing_zeros() as usize;

            while material_y < CHUNK_SIZE {
                self.materials_layer[material_z << CHUNK_SIZE_BITS | material_y] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
                required_materials &= (-2i64 as u64) << material_y;
                material_y = required_materials.trailing_zeros() as usize;
            }
        }
    }


    fn add_north_south_layer(&mut self, side: usize, material_z: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = MaterialsData::split_map(to_mesh_faces_map, side, material_z);
        for material_x in 0..CHUNK_SIZE {
            let mut material_y: usize = to_mesh_faces_map[material_x].trailing_zeros() as usize;

            while material_y < CHUNK_SIZE {
                let material: i8 = self.materials_layer[material_x << CHUNK_SIZE_BITS | material_y];
                let face_end_y: usize = self.grow_face_1st_direction(to_mesh_faces_map[material_x], material_y + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_x: usize = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_x - material_x);
                material_y = to_mesh_faces_map[material_x].trailing_zeros() as usize;
            }
        }
    }

    fn add_top_bottom_layer(&mut self, side: usize, material_y: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = MaterialsData::split_map(to_mesh_faces_map, side, material_y);
        for material_x in 0..CHUNK_SIZE {
            let mut material_z: usize = to_mesh_faces_map[material_x].trailing_zeros() as usize;

            while material_z < CHUNK_SIZE {
                let material: i8 = self.materials_layer[material_x << CHUNK_SIZE_BITS | material_z];
                let face_end_z: usize = self.grow_face_1st_direction(to_mesh_faces_map[material_x], material_z + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_z - material_z + 1, material_z);
                let face_end_x: usize = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_z, face_end_z, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_face(side, material_x, material_y, material_z, material, face_end_x - material_x, face_end_z - material_z);
                material_z = to_mesh_faces_map[material_x].trailing_zeros() as usize;
            }
        }
    }

    fn add_west_east_layer(&mut self, side: usize, material_x: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6]) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = MaterialsData::split_map(to_mesh_faces_map, side, material_x);
        for material_z in 0..CHUNK_SIZE {
            let mut material_y: usize = to_mesh_faces_map[material_z].trailing_zeros() as usize;

            while material_y < CHUNK_SIZE {
                let material: i8 = self.materials_layer[material_z << CHUNK_SIZE_BITS | material_y];
                let face_end_y: usize = self.grow_face_1st_direction(to_mesh_faces_map[material_z], material_y + 1, material_z, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_z: usize = self.grow_face_2nd_direction(to_mesh_faces_map, material_z + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_z, face_end_z);
                self.add_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_z - material_z);
                material_y = to_mesh_faces_map[material_z].trailing_zeros() as usize;
            }
        }
    }


    fn add_face(&mut self, side: usize, material_x: usize, material_y: usize, material_z: usize, material: i8, face_size_1: usize, face_size_2: usize) {
        if MeshGenerator::is_glass(material) {
            self.add_face_to_vertices(GLASS_INDEX, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
        } else if material == WATER {
            self.add_face_to_vertices(WATER_INDEX, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
        } else {
            self.add_face_to_vertices(side, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
        }
    }

    fn add_face_to_vertices(&mut self, vertices_index: usize, side: usize, material_x: usize, material_y: usize, material_z: usize, material: i8, face_size_1: usize, face_size_2: usize) {
        let vertices: &mut Vec<i32> = &mut self.vertices[vertices_index];
        vertices.push(self.x_start | material_x as i32);
        vertices.push(self.y_start | material_y as i32);
        vertices.push(self.z_start | material_z as i32);
        vertices.push(((MATERIAL_PROPERTIES[material as u8 as usize] as usize) << 24 | face_size_1 << 17 | face_size_2 << 11 | side << 8 | (material as u8 as usize)) as i32);
    }

    fn grow_face_1st_direction(&self, to_mesh_faces: u64, mut grow_start: usize, fixed_start: usize, material: i8) -> usize {
        while grow_start < CHUNK_SIZE {
            let index: usize = fixed_start << CHUNK_SIZE_BITS | grow_start;
            if to_mesh_faces & 1 << grow_start == 0 || self.materials_layer[index] != material { return grow_start - 1; }
            grow_start += 1;
        }
        CHUNK_SIZE - 1
    }

    fn grow_face_2nd_direction(&self, to_mesh_faces_map: &[u64], mut grow_start: usize, mask: u64, fixed_start: usize, fixed_end: usize, material: i8) -> usize {
        while grow_start < CHUNK_SIZE && (to_mesh_faces_map[grow_start] & mask) == mask {
            for index in fixed_start..=fixed_end {
                if self.materials_layer[grow_start << CHUNK_SIZE_BITS | index] != material { return grow_start - 1; }
            }
            grow_start += 1;
        }
        grow_start - 1
    }

    fn remove_from_map(to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut mask: u64, start: usize, end: usize) {
        mask = !mask;
        for index in start..=end {
            to_mesh_faces_map[index] &= mask;
        }
    }

    fn is_glass(material: i8) -> bool {
        material as u8 >= 125 && material as u8 <= 132
    }

    fn split_map(to_mesh_faces_map: &[u64; CHUNK_SIZE * CHUNK_SIZE * 6], side: usize, in_chunk: usize) -> &[u64; CHUNK_SIZE] {
        let index: usize = side * CHUNK_SIZE * CHUNK_SIZE + in_chunk * CHUNK_SIZE;
        <&[u64; 64]>::try_from(&to_mesh_faces_map[index..index + CHUNK_SIZE]).unwrap()
    }

    fn has_opaque_mesh(&self) -> bool {
        for vertices in &self.vertices[0..SIDES_INDEX] {
            if vertices.len() != 0 { return true; }
        }
        false
    }
}

impl MeshGenerator {
    fn add_side_layers(&mut self) {
        let to_mesh_faces_map: &mut [u64; CHUNK_SIZE] = &mut [u64::MAX; CHUNK_SIZE];
        self.copy_materials_north_south_side_layer(0);
        self.add_north_south_side_layer(SOUTH, 0, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_north_south_side_layer(CHUNK_SIZE - 1);
        self.add_north_south_side_layer(NORTH, CHUNK_SIZE - 1, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_top_bottom_side_layer(0);
        self.add_top_bottom_side_layer(BOTTOM, 0, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_top_bottom_side_layer(CHUNK_SIZE - 1);
        self.add_top_bottom_side_layer(TOP, CHUNK_SIZE - 1, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_west_east_side_layer(0);
        self.add_west_east_side_layer(EAST, 0, to_mesh_faces_map);

        to_mesh_faces_map.fill(u64::MAX);
        self.copy_materials_west_east_side_layer(CHUNK_SIZE - 1);
        self.add_west_east_side_layer(WEST, CHUNK_SIZE - 1, to_mesh_faces_map);
    }

    fn add_side_face(&mut self, side: usize, material_x: usize, material_y: usize, material_z: usize, material: i8, face_size_1: usize, face_size_2: usize) {
        if MATERIAL_PROPERTIES[material as u8 as usize] & TRANSPARENT != 0 { return; }
        self.add_face_to_vertices(SIDES_INDEX, side, material_x, material_y, material_z, material, face_size_1, face_size_2);
    }


    fn copy_materials_north_south_side_layer(&mut self, material_z: usize) {
        for material_x in 0..CHUNK_SIZE {
            for material_y in 0..CHUNK_SIZE {
                self.materials_layer[material_x << CHUNK_SIZE_BITS | material_y] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
            }
        }
    }

    fn copy_materials_top_bottom_side_layer(&mut self, material_y: usize) {
        for material_x in 0..CHUNK_SIZE {
            for material_z in 0..CHUNK_SIZE {
                self.materials_layer[material_x << CHUNK_SIZE_BITS | material_z] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
            }
        }
    }

    fn copy_materials_west_east_side_layer(&mut self, material_x: usize) {
        for material_z in 0..CHUNK_SIZE {
            for material_y in 0..CHUNK_SIZE {
                self.materials_layer[material_z << CHUNK_SIZE_BITS | material_y] = self.uncompressed_materials[get_uncompressed_index(material_x, material_y, material_z)];
            }
        }
    }


    fn add_north_south_side_layer(&mut self, side: usize, material_z: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE]) {
        for material_x in 0..CHUNK_SIZE {
            let mut material_y: usize = to_mesh_faces_map[material_x].trailing_zeros() as usize;

            while material_y < CHUNK_SIZE {
                let material: i8 = self.materials_layer[material_x << CHUNK_SIZE_BITS | material_y];
                let face_end_y: usize = self.grow_face_1st_direction(to_mesh_faces_map[material_x], material_y + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_x: usize = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_side_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_x - material_x);
                material_y = to_mesh_faces_map[material_x].trailing_zeros() as usize;
            }
        }
    }

    fn add_top_bottom_side_layer(&mut self, side: usize, material_y: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE]) {
        for material_x in 0..CHUNK_SIZE {
            let mut material_z: usize = to_mesh_faces_map[material_x].trailing_zeros() as usize;

            while material_z < CHUNK_SIZE {
                let material: i8 = self.materials_layer[material_x << CHUNK_SIZE_BITS | material_z];
                let face_end_z: usize = self.grow_face_1st_direction(to_mesh_faces_map[material_x], material_z + 1, material_x, material);
                let mask: u64 = MaterialsData::get_mask(face_end_z - material_z + 1, material_z);
                let face_end_x: usize = self.grow_face_2nd_direction(to_mesh_faces_map, material_x + 1, mask, material_z, face_end_z, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_x, face_end_x);
                self.add_side_face(side, material_x, material_y, material_z, material, face_end_x - material_x, face_end_z - material_z);
                material_z = to_mesh_faces_map[material_x].trailing_zeros() as usize;
            }
        }
    }

    fn add_west_east_side_layer(&mut self, side: usize, material_x: usize, to_mesh_faces_map: &mut [u64; CHUNK_SIZE]) {
        for material_z in 0..CHUNK_SIZE {
            let mut material_y: usize = to_mesh_faces_map[material_z].trailing_zeros() as usize;

            while material_y < CHUNK_SIZE {
                let material: i8 = self.materials_layer[material_z << CHUNK_SIZE_BITS | material_y];
                let face_end_y: usize = self.grow_face_1st_direction(to_mesh_faces_map[material_z], material_y + 1, material_z, material);
                let mask: u64 = MaterialsData::get_mask(face_end_y - material_y + 1, material_y);
                let face_end_z: usize = self.grow_face_2nd_direction(to_mesh_faces_map, material_z + 1, mask, material_y, face_end_y, material);

                MeshGenerator::remove_from_map(to_mesh_faces_map, mask, material_z, face_end_z);
                self.add_side_face(side, material_x, material_y, material_z, material, face_end_y - material_y, face_end_z - material_z);
                material_y = to_mesh_faces_map[material_z].trailing_zeros() as usize;
            }
        }
    }
}

static MATERIAL_PROPERTIES: [u32; 140] = [11, 6, 0, 0, 15, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 14, 14, 14, 14, 14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0];

const TRANSPARENT: u32 = 2;
const OCCLUDES_SELF_ONLY: u32 = 6;

const WATER: i8 = 4;
const SIDES_INDEX: usize = 6;
const WATER_INDEX: usize = 7;
const GLASS_INDEX: usize = 8;

