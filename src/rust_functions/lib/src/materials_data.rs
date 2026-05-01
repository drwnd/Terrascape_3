use super::mesh_generator;

pub struct MaterialsData<'a> {
    pub(crate) data: &'a [i8],
}

impl MaterialsData<'_> {
    pub fn fill_uncompressed_materials(&self, uncompressed_materials: &mut [i8; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE],
                                       mut size_bits: usize, mut start_index: usize,
                                       in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == HOMOGENOUS {
            let size: usize = 1 << size_bits * 3;
            let material: i8 = self.data[start_index + 1];
            start_index = get_uncompressed_index(in_chunk_x, in_chunk_y, in_chunk_z);
            uncompressed_materials[start_index..start_index + size].fill(material);
            return;
        }
        if identifier == DETAIL {
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 0, in_chunk_y + 0, in_chunk_z + 0)] = self.data[start_index + 1];
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 0, in_chunk_y + 1, in_chunk_z + 0)] = self.data[start_index + 2];
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 0, in_chunk_y + 0, in_chunk_z + 1)] = self.data[start_index + 3];
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 0, in_chunk_y + 1, in_chunk_z + 1)] = self.data[start_index + 4];
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 1, in_chunk_y + 0, in_chunk_z + 0)] = self.data[start_index + 5];
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 1, in_chunk_y + 1, in_chunk_z + 0)] = self.data[start_index + 6];
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 1, in_chunk_y + 0, in_chunk_z + 1)] = self.data[start_index + 7];
            uncompressed_materials[get_uncompressed_index(in_chunk_x + 1, in_chunk_y + 1, in_chunk_z + 1)] = self.data[start_index + 8];
            return;
        }

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + SPLITTER_BYTE_SIZE, in_chunk_x, in_chunk_y, in_chunk_z);
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + self.get_offset(start_index + 1), in_chunk_x, in_chunk_y, in_chunk_z + next_size);
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + self.get_offset(start_index + 4), in_chunk_x, in_chunk_y + next_size, in_chunk_z);
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + self.get_offset(start_index + 7), in_chunk_x, in_chunk_y + next_size, in_chunk_z + next_size);
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + self.get_offset(start_index + 10), in_chunk_x + next_size, in_chunk_y, in_chunk_z);
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + self.get_offset(start_index + 13), in_chunk_x + next_size, in_chunk_y, in_chunk_z + next_size);
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + self.get_offset(start_index + 16), in_chunk_x + next_size, in_chunk_y + next_size, in_chunk_z);
            self.fill_uncompressed_materials(uncompressed_materials, size_bits, start_index + self.get_offset(start_index + 19), in_chunk_x + next_size, in_chunk_y + next_size, in_chunk_z + next_size);
        } else { panic!() }
    }

    pub fn generate_to_mesh_faces_maps(&self,
                                       to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6], uncompressed_materials: &[i8; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE],
                                       adjacent_chunk_layers: &[&[i8]; 6], mut size_bits: usize, start_index: usize,
                                       in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + SPLITTER_BYTE_SIZE, in_chunk_x, in_chunk_y, in_chunk_z);
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + self.get_offset(start_index + 1), in_chunk_x, in_chunk_y, in_chunk_z + next_size);
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + self.get_offset(start_index + 4), in_chunk_x, in_chunk_y + next_size, in_chunk_z);
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + self.get_offset(start_index + 7), in_chunk_x, in_chunk_y + next_size, in_chunk_z + next_size);
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + self.get_offset(start_index + 10), in_chunk_x + next_size, in_chunk_y, in_chunk_z);
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + self.get_offset(start_index + 13), in_chunk_x + next_size, in_chunk_y, in_chunk_z + next_size);
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + self.get_offset(start_index + 16), in_chunk_x + next_size, in_chunk_y + next_size, in_chunk_z);
            self.generate_to_mesh_faces_maps(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, size_bits, start_index + self.get_offset(start_index + 19), in_chunk_x + next_size, in_chunk_y + next_size, in_chunk_z + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            let material: i8 = self.data[start_index + 1];
            if material == AIR { return; }
            let length: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_north_layer(MaterialsData::split_map(to_mesh_faces_map, NORTH, in_chunk_z + length - 1), adjacent_chunk_layers, size_bits, material, in_chunk_x, in_chunk_y, in_chunk_z + length);
            self.generate_to_mesh_faces_homogenous_top_layer(MaterialsData::split_map(to_mesh_faces_map, TOP, in_chunk_y + length - 1), adjacent_chunk_layers, size_bits, material, in_chunk_x, in_chunk_y + length, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_west_layer(MaterialsData::split_map(to_mesh_faces_map, WEST, in_chunk_x + length - 1), adjacent_chunk_layers, size_bits, material, in_chunk_x + length, in_chunk_y, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_south_layer(MaterialsData::split_map(to_mesh_faces_map, SOUTH, in_chunk_z), adjacent_chunk_layers, size_bits, material, in_chunk_x, in_chunk_y, in_chunk_z.wrapping_sub(1));
            self.generate_to_mesh_faces_homogenous_bottom_layer(MaterialsData::split_map(to_mesh_faces_map, BOTTOM, in_chunk_y), adjacent_chunk_layers, size_bits, material, in_chunk_x, in_chunk_y.wrapping_sub(1), in_chunk_z);
            self.generate_to_mesh_faces_homogenous_east_layer(MaterialsData::split_map(to_mesh_faces_map, EAST, in_chunk_x), adjacent_chunk_layers, size_bits, material, in_chunk_x.wrapping_sub(1), in_chunk_y, in_chunk_z);
            return;
        }
        if identifier == DETAIL {
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y, in_chunk_z);
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y, in_chunk_z + 1);
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y + 1, in_chunk_z);
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y + 1, in_chunk_z + 1);
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x + 1, in_chunk_y, in_chunk_z);
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x + 1, in_chunk_y, in_chunk_z + 1);
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x + 1, in_chunk_y + 1, in_chunk_z);
            self.generate_to_mesh_faces_detail(to_mesh_faces_map, uncompressed_materials, adjacent_chunk_layers, in_chunk_x + 1, in_chunk_y + 1, in_chunk_z + 1);
        } else { panic!() }
    }
}

// To mesh faces functions
impl MaterialsData<'_> {
    fn generate_to_mesh_faces_homogenous_north_layer(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], adjacent_chunk_layers: &[&[i8]; 6],
                                                     size_bits: usize, material: i8, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        if in_chunk_z == CHUNK_SIZE {
            let adjacent_chunk_layer: &[i8] = adjacent_chunk_layers[NORTH];
            let start_index: usize = MaterialsData::start_index_of_2d(adjacent_chunk_layer, in_chunk_x, in_chunk_y, CHUNK_SIZE_BITS, size_bits);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index, size_bits, material, in_chunk_x, in_chunk_y);
            return;
        }
        let start_index: usize = self.start_index_of(in_chunk_x, in_chunk_y, in_chunk_z, size_bits);
        self.generate_to_mesh_faces_homogenous_north_layer_inside(to_mesh_faces_map, size_bits, material, start_index, in_chunk_x, in_chunk_y)
    }

    fn generate_to_mesh_faces_homogenous_north_layer_inside(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut size_bits: usize, material: i8,
                                                            start_index: usize, in_chunk_x: usize, in_chunk_y: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_north_layer_inside(to_mesh_faces_map, size_bits, material, start_index + SPLITTER_BYTE_SIZE, in_chunk_x, in_chunk_y);
            self.generate_to_mesh_faces_homogenous_north_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 4), in_chunk_x, in_chunk_y + next_size);
            self.generate_to_mesh_faces_homogenous_north_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 10), in_chunk_x + next_size, in_chunk_y);
            self.generate_to_mesh_faces_homogenous_north_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 16), in_chunk_x + next_size, in_chunk_y + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            MaterialsData::fill_to_mesh_faces_map_homogenous(to_mesh_faces_map, size_bits, material, self.data[start_index + 1], in_chunk_x, in_chunk_y);
            return;
        }
        if identifier == DETAIL {
            if mesh_generator::is_visible(material, self.data[start_index + 1]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 2]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_y + 1; }
            if mesh_generator::is_visible(material, self.data[start_index + 5]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 6]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_y + 1; }
        } else { panic!() }
    }


    fn generate_to_mesh_faces_homogenous_south_layer(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], adjacent_chunk_layers: &[&[i8]; 6],
                                                     size_bits: usize, material: i8, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        if in_chunk_z == usize::MAX {
            let adjacent_chunk_layer: &[i8] = adjacent_chunk_layers[SOUTH];
            let start_index: usize = MaterialsData::start_index_of_2d(adjacent_chunk_layer, in_chunk_x, in_chunk_y, CHUNK_SIZE_BITS, size_bits);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index, size_bits, material, in_chunk_x, in_chunk_y);
            return;
        }
        let start_index: usize = self.start_index_of(in_chunk_x, in_chunk_y, in_chunk_z, size_bits);
        self.generate_to_mesh_faces_homogenous_south_layer_inside(to_mesh_faces_map, size_bits, material, start_index, in_chunk_x, in_chunk_y)
    }

    fn generate_to_mesh_faces_homogenous_south_layer_inside(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut size_bits: usize, material: i8,
                                                            start_index: usize, in_chunk_x: usize, in_chunk_y: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_south_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 1), in_chunk_x, in_chunk_y);
            self.generate_to_mesh_faces_homogenous_south_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 7), in_chunk_x, in_chunk_y + next_size);
            self.generate_to_mesh_faces_homogenous_south_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 13), in_chunk_x + next_size, in_chunk_y);
            self.generate_to_mesh_faces_homogenous_south_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 19), in_chunk_x + next_size, in_chunk_y + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            MaterialsData::fill_to_mesh_faces_map_homogenous(to_mesh_faces_map, size_bits, material, self.data[start_index + 1], in_chunk_x, in_chunk_y);
            return;
        }
        if identifier == DETAIL {
            if mesh_generator::is_visible(material, self.data[start_index + 3]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 4]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_y + 1; }
            if mesh_generator::is_visible(material, self.data[start_index + 7]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 8]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_y + 1; }
        } else { panic!() }
    }


    fn generate_to_mesh_faces_homogenous_top_layer(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], adjacent_chunk_layers: &[&[i8]; 6],
                                                   size_bits: usize, material: i8, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        if in_chunk_y == CHUNK_SIZE {
            let adjacent_chunk_layer: &[i8] = adjacent_chunk_layers[TOP];
            let start_index: usize = MaterialsData::start_index_of_2d(adjacent_chunk_layer, in_chunk_x, in_chunk_z, CHUNK_SIZE_BITS, size_bits);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index, size_bits, material, in_chunk_x, in_chunk_z);
            return;
        }
        let start_index: usize = self.start_index_of(in_chunk_x, in_chunk_y, in_chunk_z, size_bits);
        self.generate_to_mesh_faces_homogenous_top_layer_inside(to_mesh_faces_map, size_bits, material, start_index, in_chunk_x, in_chunk_z)
    }

    fn generate_to_mesh_faces_homogenous_top_layer_inside(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut size_bits: usize, material: i8,
                                                          start_index: usize, in_chunk_x: usize, in_chunk_z: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_top_layer_inside(to_mesh_faces_map, size_bits, material, start_index + SPLITTER_BYTE_SIZE, in_chunk_x, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_top_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 1), in_chunk_x, in_chunk_z + next_size);
            self.generate_to_mesh_faces_homogenous_top_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 10), in_chunk_x + next_size, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_top_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 13), in_chunk_x + next_size, in_chunk_z + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            MaterialsData::fill_to_mesh_faces_map_homogenous(to_mesh_faces_map, size_bits, material, self.data[start_index + 1], in_chunk_x, in_chunk_z);
            return;
        }
        if identifier == DETAIL {
            if mesh_generator::is_visible(material, self.data[start_index + 1]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_z + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 3]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_z + 1; }
            if mesh_generator::is_visible(material, self.data[start_index + 5]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_z + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 7]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_z + 1; }
        } else { panic!() }
    }


    fn generate_to_mesh_faces_homogenous_bottom_layer(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], adjacent_chunk_layers: &[&[i8]; 6],
                                                      size_bits: usize, material: i8, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        if in_chunk_y == usize::MAX {
            let adjacent_chunk_layer: &[i8] = adjacent_chunk_layers[BOTTOM];
            let start_index: usize = MaterialsData::start_index_of_2d(adjacent_chunk_layer, in_chunk_x, in_chunk_z, CHUNK_SIZE_BITS, size_bits);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index, size_bits, material, in_chunk_x, in_chunk_z);
            return;
        }
        let start_index: usize = self.start_index_of(in_chunk_x, in_chunk_y, in_chunk_z, size_bits);
        self.generate_to_mesh_faces_homogenous_bottom_layer_inside(to_mesh_faces_map, size_bits, material, start_index, in_chunk_x, in_chunk_z)
    }

    fn generate_to_mesh_faces_homogenous_bottom_layer_inside(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut size_bits: usize, material: i8,
                                                             start_index: usize, in_chunk_x: usize, in_chunk_z: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_bottom_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 4), in_chunk_x, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_bottom_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 7), in_chunk_x, in_chunk_z + next_size);
            self.generate_to_mesh_faces_homogenous_bottom_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 16), in_chunk_x + next_size, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_bottom_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 19), in_chunk_x + next_size, in_chunk_z + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            MaterialsData::fill_to_mesh_faces_map_homogenous(to_mesh_faces_map, size_bits, material, self.data[start_index + 1], in_chunk_x, in_chunk_z);
            return;
        }
        if identifier == DETAIL {
            if mesh_generator::is_visible(material, self.data[start_index + 2]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_z + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 4]) { to_mesh_faces_map[in_chunk_x + 0] |= 1 << in_chunk_z + 1; }
            if mesh_generator::is_visible(material, self.data[start_index + 6]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_z + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 8]) { to_mesh_faces_map[in_chunk_x + 1] |= 1 << in_chunk_z + 1; }
        } else { panic!() }
    }


    fn generate_to_mesh_faces_homogenous_west_layer(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], adjacent_chunk_layers: &[&[i8]; 6],
                                                    size_bits: usize, material: i8, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        if in_chunk_x == CHUNK_SIZE {
            let adjacent_chunk_layer: &[i8] = adjacent_chunk_layers[WEST];
            let start_index: usize = MaterialsData::start_index_of_2d(adjacent_chunk_layer, in_chunk_z, in_chunk_y, CHUNK_SIZE_BITS, size_bits);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index, size_bits, material, in_chunk_z, in_chunk_y);
            return;
        }
        let start_index: usize = self.start_index_of(in_chunk_x, in_chunk_y, in_chunk_z, size_bits);
        self.generate_to_mesh_faces_homogenous_west_layer_inside(to_mesh_faces_map, size_bits, material, start_index, in_chunk_y, in_chunk_z)
    }

    fn generate_to_mesh_faces_homogenous_west_layer_inside(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut size_bits: usize, material: i8,
                                                           start_index: usize, in_chunk_y: usize, in_chunk_z: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_west_layer_inside(to_mesh_faces_map, size_bits, material, start_index + SPLITTER_BYTE_SIZE, in_chunk_y, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_west_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 1), in_chunk_y, in_chunk_z + next_size);
            self.generate_to_mesh_faces_homogenous_west_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 4), in_chunk_y + next_size, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_west_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 7), in_chunk_y + next_size, in_chunk_z + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            MaterialsData::fill_to_mesh_faces_map_homogenous(to_mesh_faces_map, size_bits, material, self.data[start_index + 1], in_chunk_z, in_chunk_y);
            return;
        }
        if identifier == DETAIL {
            if mesh_generator::is_visible(material, self.data[start_index + 1]) { to_mesh_faces_map[in_chunk_z + 0] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 2]) { to_mesh_faces_map[in_chunk_z + 0] |= 1 << in_chunk_y + 1; }
            if mesh_generator::is_visible(material, self.data[start_index + 3]) { to_mesh_faces_map[in_chunk_z + 1] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 4]) { to_mesh_faces_map[in_chunk_z + 1] |= 1 << in_chunk_y + 1; }
        } else { panic!() }
    }


    fn generate_to_mesh_faces_homogenous_east_layer(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], adjacent_chunk_layers: &[&[i8]; 6],
                                                    size_bits: usize, material: i8, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        if in_chunk_x == usize::MAX {
            let adjacent_chunk_layer: &[i8] = adjacent_chunk_layers[EAST];
            let start_index: usize = MaterialsData::start_index_of_2d(adjacent_chunk_layer, in_chunk_z, in_chunk_y, CHUNK_SIZE_BITS, size_bits);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index, size_bits, material, in_chunk_z, in_chunk_y);
            return;
        }
        let start_index: usize = self.start_index_of(in_chunk_x, in_chunk_y, in_chunk_z, size_bits);
        self.generate_to_mesh_faces_homogenous_east_layer_inside(to_mesh_faces_map, size_bits, material, start_index, in_chunk_y, in_chunk_z)
    }

    fn generate_to_mesh_faces_homogenous_east_layer_inside(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], mut size_bits: usize, material: i8,
                                                           start_index: usize, in_chunk_y: usize, in_chunk_z: usize) {
        let identifier: i8 = self.get_identifier(start_index);

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_east_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 10), in_chunk_y, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_east_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 13), in_chunk_y, in_chunk_z + next_size);
            self.generate_to_mesh_faces_homogenous_east_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 16), in_chunk_y + next_size, in_chunk_z);
            self.generate_to_mesh_faces_homogenous_east_layer_inside(to_mesh_faces_map, size_bits, material, start_index + self.get_offset(start_index + 19), in_chunk_y + next_size, in_chunk_z + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            MaterialsData::fill_to_mesh_faces_map_homogenous(to_mesh_faces_map, size_bits, material, self.data[start_index + 1], in_chunk_z, in_chunk_y);
            return;
        }
        if identifier == DETAIL {
            if mesh_generator::is_visible(material, self.data[start_index + 5]) { to_mesh_faces_map[in_chunk_z + 0] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 6]) { to_mesh_faces_map[in_chunk_z + 0] |= 1 << in_chunk_y + 1; }
            if mesh_generator::is_visible(material, self.data[start_index + 7]) { to_mesh_faces_map[in_chunk_z + 1] |= 1 << in_chunk_y + 0; }
            if mesh_generator::is_visible(material, self.data[start_index + 8]) { to_mesh_faces_map[in_chunk_z + 1] |= 1 << in_chunk_y + 1; }
        } else { panic!() }
    }


    fn generate_to_mesh_faces_homogenous_side_layer(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE], adjacent_chunk_layer: &[i8],
                                                    start_index: usize, mut size_bits: usize, material: i8, in_chunk_a: usize, in_chunk_b: usize) {
        let identifier: i8 = adjacent_chunk_layer[start_index] & IDENTIFIER_MASK;

        if identifier == SPLITTER {
            size_bits -= 1;
            let next_size: usize = 1 << size_bits;
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index + SPLITTER_BYTE_SIZE_2D, size_bits, material, in_chunk_a, in_chunk_b);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index + MaterialsData::get_offset_2d(adjacent_chunk_layer, start_index + 1), size_bits, material, in_chunk_a, in_chunk_b + next_size);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index + MaterialsData::get_offset_2d(adjacent_chunk_layer, start_index + 4), size_bits, material, in_chunk_a + next_size, in_chunk_b);
            self.generate_to_mesh_faces_homogenous_side_layer(to_mesh_faces_map, adjacent_chunk_layer, start_index + MaterialsData::get_offset_2d(adjacent_chunk_layer, start_index + 7), size_bits, material, in_chunk_a + next_size, in_chunk_b + next_size);
            return;
        }
        if identifier == HOMOGENOUS {
            let occluding_material: i8 = adjacent_chunk_layer[start_index + 1];
            MaterialsData::fill_to_mesh_faces_map_homogenous(to_mesh_faces_map, size_bits, material, occluding_material, in_chunk_a, in_chunk_b);
            return;
        }
        if identifier == DETAIL {
            if mesh_generator::is_visible(material, adjacent_chunk_layer[start_index + 1]) { to_mesh_faces_map[in_chunk_a + 0] |= 1 << in_chunk_b + 0 }
            if mesh_generator::is_visible(material, adjacent_chunk_layer[start_index + 2]) { to_mesh_faces_map[in_chunk_a + 0] |= 1 << in_chunk_b + 1 }
            if mesh_generator::is_visible(material, adjacent_chunk_layer[start_index + 3]) { to_mesh_faces_map[in_chunk_a + 1] |= 1 << in_chunk_b + 0 }
            if mesh_generator::is_visible(material, adjacent_chunk_layer[start_index + 4]) { to_mesh_faces_map[in_chunk_a + 1] |= 1 << in_chunk_b + 1 }
        } else { panic!() }
    }

    fn fill_to_mesh_faces_map_homogenous(to_mesh_faces_map: &mut [u64; CHUNK_SIZE], size_bits: usize, material: i8, occluding_material: i8, in_chunk_a: usize, in_chunk_b: usize) {
        if !mesh_generator::is_visible(material, occluding_material) { return; }
        let length: usize = 1 << size_bits;
        let mask: u64 = MaterialsData::get_mask(length, in_chunk_b);
        for a in in_chunk_a..in_chunk_a + length {
            to_mesh_faces_map[a] |= mask;
        }
    }

    fn generate_to_mesh_faces_detail(&self, to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6], uncompressed_materials: &[i8; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE],
                                     adjacent_chunk_layers: &[&[i8]; 6], in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) {
        let material: i8 = uncompressed_materials[get_uncompressed_index(in_chunk_x, in_chunk_y, in_chunk_z)];
        if material == AIR { return; }

        let north_material: i8 = MaterialsData::get_material(uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y, in_chunk_z + 1);
        let top_material: i8 = MaterialsData::get_material(uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y + 1, in_chunk_z);
        let west_material: i8 = MaterialsData::get_material(uncompressed_materials, adjacent_chunk_layers, in_chunk_x + 1, in_chunk_y, in_chunk_z);
        let south_material: i8 = MaterialsData::get_material(uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y, in_chunk_z.wrapping_sub(1));
        let bottom_material: i8 = MaterialsData::get_material(uncompressed_materials, adjacent_chunk_layers, in_chunk_x, in_chunk_y.wrapping_sub(1), in_chunk_z);
        let east_material: i8 = MaterialsData::get_material(uncompressed_materials, adjacent_chunk_layers, in_chunk_x.wrapping_sub(1), in_chunk_y, in_chunk_z);

        if mesh_generator::is_visible(material, north_material) { MaterialsData::split_map(to_mesh_faces_map, NORTH, in_chunk_z)[in_chunk_x] |= 1 << in_chunk_y; }
        if mesh_generator::is_visible(material, top_material) { MaterialsData::split_map(to_mesh_faces_map, TOP, in_chunk_y)[in_chunk_x] |= 1 << in_chunk_z; }
        if mesh_generator::is_visible(material, west_material) { MaterialsData::split_map(to_mesh_faces_map, WEST, in_chunk_x)[in_chunk_z] |= 1 << in_chunk_y; }
        if mesh_generator::is_visible(material, south_material) { MaterialsData::split_map(to_mesh_faces_map, SOUTH, in_chunk_z)[in_chunk_x] |= 1 << in_chunk_y; }
        if mesh_generator::is_visible(material, bottom_material) { MaterialsData::split_map(to_mesh_faces_map, BOTTOM, in_chunk_y)[in_chunk_x] |= 1 << in_chunk_z; }
        if mesh_generator::is_visible(material, east_material) { MaterialsData::split_map(to_mesh_faces_map, EAST, in_chunk_x)[in_chunk_z] |= 1 << in_chunk_y; }
    }

    fn get_material(uncompressed_materials: &[i8; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE], adjacent_chunk_layers: &[&[i8]; 6], in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) -> i8 {
        if in_chunk_x == usize::MAX { return MaterialsData::get_material_2d(adjacent_chunk_layers[EAST], in_chunk_z, in_chunk_y); }
        if in_chunk_y == usize::MAX { return MaterialsData::get_material_2d(adjacent_chunk_layers[BOTTOM], in_chunk_x, in_chunk_z); }
        if in_chunk_z == usize::MAX { return MaterialsData::get_material_2d(adjacent_chunk_layers[SOUTH], in_chunk_x, in_chunk_y); }
        if in_chunk_x == CHUNK_SIZE { return MaterialsData::get_material_2d(adjacent_chunk_layers[WEST], in_chunk_z, in_chunk_y); }
        if in_chunk_y == CHUNK_SIZE { return MaterialsData::get_material_2d(adjacent_chunk_layers[TOP], in_chunk_x, in_chunk_z); }
        if in_chunk_z == CHUNK_SIZE { return MaterialsData::get_material_2d(adjacent_chunk_layers[NORTH], in_chunk_x, in_chunk_y); }

        uncompressed_materials[get_uncompressed_index(in_chunk_x, in_chunk_y, in_chunk_z)]
    }
}

// Helper functions
impl MaterialsData<'_> {
    fn start_index_of_2d(data: &[i8], in_chunk_a: usize, in_chunk_b: usize, mut size_bits: usize, target_size_bits: usize) -> usize {
        let mut index: usize = 0;
        loop {
            let identifier: i8 = data[index] & IDENTIFIER_MASK;
            if size_bits <= target_size_bits || identifier == HOMOGENOUS || identifier == DETAIL {
                return index;
            }
            size_bits -= 1;
            index += MaterialsData::get_offset_2d_of(data, index, in_chunk_a, in_chunk_b, size_bits);
        }
    }

    fn get_offset_2d_of(data: &[i8], splitter_index: usize, in_chunk_a: usize, in_chunk_b: usize, size_bits: usize) -> usize {
        let in_splitter_index: usize = 3 * ((in_chunk_a >> size_bits & 1) << 1 | (in_chunk_b >> size_bits & 1));
        if in_splitter_index == 0 { return SPLITTER_BYTE_SIZE_2D; }
        MaterialsData::get_offset_2d(data, splitter_index + in_splitter_index - 2)
    }

    fn get_offset_2d(data: &[i8], index: usize) -> usize {
        (data[index] as u8 as usize) << 16 | (data[index + 1] as u8 as usize) << 8 | (data[index + 2] as u8 as usize)
    }

    fn get_material_2d(data: &[i8], in_chunk_a: usize, in_chunk_b: usize) -> i8 {
        let mut index: usize = 0;
        let mut size_bits: usize = CHUNK_SIZE_BITS;
        loop {
            let identifier: i8 = data[index] & IDENTIFIER_MASK;

            if identifier == HOMOGENOUS { return data[index + 1]; }
            if identifier == DETAIL { return data[index + ((in_chunk_a & 1) << 1 | (in_chunk_b & 1)) + 1]; }
            if identifier == SPLITTER {
                size_bits -= 1;
                index += MaterialsData::get_offset_2d_of(data, index, in_chunk_a, in_chunk_b, size_bits);
            } else { panic!() }
        }
    }

    fn start_index_of(&self, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize, target_size_bits: usize) -> usize {
        let mut index: usize = 0;
        let mut size_bits: usize = CHUNK_SIZE_BITS;
        loop {
            let identifier: i8 = self.get_identifier(index);
            if size_bits <= target_size_bits || identifier == HOMOGENOUS || identifier == DETAIL {
                return index;
            }
            size_bits -= 1;
            index += self.get_offset_at(index, in_chunk_x, in_chunk_y, in_chunk_z, size_bits);
        }
    }

    fn get_offset_at(&self, splitter_index: usize, in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize, size_bits: usize) -> usize {
        let in_splitter_index: usize = 3 * ((in_chunk_x >> size_bits & 1) << 2 | (in_chunk_y >> size_bits & 1) << 1 | (in_chunk_z >> size_bits & 1));
        if in_splitter_index == 0 {
            return SPLITTER_BYTE_SIZE;
        }
        self.get_offset(splitter_index + in_splitter_index - 2)
    }

    fn get_offset(&self, index: usize) -> usize {
        (self.data[index] as u8 as usize) << 16 | (self.data[index + 1] as u8 as usize) << 8 | (self.data[index + 2] as u8 as usize)
    }

    fn get_identifier(&self, start_index: usize) -> i8 {
        self.data[start_index] & IDENTIFIER_MASK
    }

    pub fn split_map(to_mesh_faces_map: &mut [u64; CHUNK_SIZE * CHUNK_SIZE * 6], side: usize, in_chunk: usize) -> &mut [u64; CHUNK_SIZE] {
        let index: usize = side * CHUNK_SIZE * CHUNK_SIZE + in_chunk * CHUNK_SIZE;
        <&mut [u64; 64]>::try_from(&mut to_mesh_faces_map[index..index + CHUNK_SIZE]).unwrap()
    }

    pub fn get_mask(length: usize, offset: usize) -> u64 {
        if length == CHUNK_SIZE { u64::MAX } else { (1 << length) - 1 << offset }
    }
}


pub fn get_uncompressed_index(in_chunk_x: usize, in_chunk_y: usize, in_chunk_z: usize) -> usize {
    (Z_ORDER_3D_TABLE_X[in_chunk_x] | Z_ORDER_3D_TABLE_Y[in_chunk_y] | Z_ORDER_3D_TABLE_Z[in_chunk_z]) as usize
}


static Z_ORDER_3D_TABLE_X: [u32; CHUNK_SIZE] = [0, 4, 32, 36, 256, 260, 288, 292, 2048, 2052, 2080, 2084, 2304, 2308, 2336, 2340, 16384, 16388, 16416, 16420, 16640, 16644, 16672, 16676, 18432, 18436, 18464, 18468, 18688, 18692, 18720, 18724, 131072, 131076, 131104, 131108, 131328, 131332, 131360, 131364, 133120, 133124, 133152, 133156, 133376, 133380, 133408, 133412, 147456, 147460, 147488, 147492, 147712, 147716, 147744, 147748, 149504, 149508, 149536, 149540, 149760, 149764, 149792, 149796];
static Z_ORDER_3D_TABLE_Y: [u32; CHUNK_SIZE] = [0, 2, 16, 18, 128, 130, 144, 146, 1024, 1026, 1040, 1042, 1152, 1154, 1168, 1170, 8192, 8194, 8208, 8210, 8320, 8322, 8336, 8338, 9216, 9218, 9232, 9234, 9344, 9346, 9360, 9362, 65536, 65538, 65552, 65554, 65664, 65666, 65680, 65682, 66560, 66562, 66576, 66578, 66688, 66690, 66704, 66706, 73728, 73730, 73744, 73746, 73856, 73858, 73872, 73874, 74752, 74754, 74768, 74770, 74880, 74882, 74896, 74898];
static Z_ORDER_3D_TABLE_Z: [u32; CHUNK_SIZE] = [0, 1, 8, 9, 64, 65, 72, 73, 512, 513, 520, 521, 576, 577, 584, 585, 4096, 4097, 4104, 4105, 4160, 4161, 4168, 4169, 4608, 4609, 4616, 4617, 4672, 4673, 4680, 4681, 32768, 32769, 32776, 32777, 32832, 32833, 32840, 32841, 33280, 33281, 33288, 33289, 33344, 33345, 33352, 33353, 36864, 36865, 36872, 36873, 36928, 36929, 36936, 36937, 37376, 37377, 37384, 37385, 37440, 37441, 37448, 37449];

pub const CHUNK_SIZE: usize = 64;
pub const CHUNK_SIZE_BITS: usize = 6;
pub const OPAQUE: i8 = 6;
pub const AIR: i8 = 0;

pub const NORTH: usize = 0;
pub const TOP: usize = 1;
pub const WEST: usize = 2;
pub const SOUTH: usize = 3;
pub const BOTTOM: usize = 4;
pub const EAST: usize = 5;

const IDENTIFIER_MASK: i8 = 0x0F;
const HOMOGENOUS: i8 = 0;
const DETAIL: i8 = 1;
const SPLITTER: i8 = 2;

const SPLITTER_BYTE_SIZE: usize = 22;
const SPLITTER_BYTE_SIZE_2D: usize = 10;
