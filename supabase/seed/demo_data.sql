-- ============================================================================
-- RentEase demo/seed data
-- ============================================================================
-- Populates universities + published, verified properties (with images and
-- rooms) plus a few reviews, so the Student search/listing screens have real
-- content to show for demos, screenshots, and manual testing — WITHOUT
-- needing a real landlord account to have gone through onboarding first.
--
-- Safe to re-run: universities/properties are idempotent via ON CONFLICT on
-- their unique `slug` column; property_images/rooms/reviews (which have no
-- natural unique column) use NOT EXISTS guards instead, so re-running this
-- script will not duplicate any of the five tables it touches.
--
-- Run with:
--   supabase db execute -f supabase/seed/demo_data.sql
-- or paste directly into the Supabase SQL editor.
--
-- These properties have landlord_id = NULL (no owning account) — they are
-- platform demo listings, not real landlord inventory, matching the fact
-- that landlord_id is nullable in the schema (ON DELETE CASCADE, no NOT
-- NULL constraint). landlord_name is still populated so the UI has
-- something sensible to display, since that column IS NOT NULL.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Universities
-- ---------------------------------------------------------------------------
INSERT INTO public.universities (slug, name, short_name, city, province, students, average_rent, latitude, longitude)
VALUES
  ('wits', 'University of the Witwatersrand', 'Wits', 'Johannesburg', 'Gauteng', 40000, 4800, -26.1929, 28.0305),
  ('up', 'University of Pretoria', 'UP', 'Pretoria', 'Gauteng', 53000, 4200, -25.7545, 28.2314),
  ('uct', 'University of Cape Town', 'UCT', 'Cape Town', 'Western Cape', 29000, 6500, -33.9577, 18.4612),
  ('su', 'Stellenbosch University', 'SU', 'Stellenbosch', 'Western Cape', 32000, 5800, -33.9321, 18.8602),
  ('uj', 'University of Johannesburg', 'UJ', 'Johannesburg', 'Gauteng', 50000, 3900, -26.1825, 27.9714),
  ('ukzn', 'University of KwaZulu-Natal', 'UKZN', 'Durban', 'KwaZulu-Natal', 44000, 3600, -29.8674, 30.9776)
ON CONFLICT (slug) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Properties (published + verified so they appear in Student search)
-- ---------------------------------------------------------------------------
INSERT INTO public.properties (
  landlord_id, landlord_name, university_id, slug, title, description, address, city, province,
  latitude, longitude, distance_km, price_per_month, deposit, bedrooms, bathrooms, rooms_available,
  furnished, verified, amenities, cover_image, rating, review_count, status
)
VALUES
(
  NULL, 'Braamfontein Student Living', (SELECT id FROM public.universities WHERE slug = 'wits'),
  'sunset-heights-braamfontein', 'Sunset Heights Braamfontein',
  'Modern, secure student apartment block two blocks from Wits main campus. Fully furnished units with fibre WiFi, on-site laundry, and 24/7 security guards at the gate.',
  '14 De Korte Street, Braamfontein', 'Johannesburg', 'Gauteng',
  -26.1919, 28.0362, 0.8, 4600, 4600, 1, 1, 3, true, true,
  ARRAY['WiFi','Furnished','24/7 Security','Laundry','Study Room','Backup Power'],
  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800', 4.6, 18, 'published'
),
(
  NULL, 'Hatfield Digs Co', (SELECT id FROM public.universities WHERE slug = 'up'),
  'the-nest-hatfield', 'The Nest Hatfield',
  'Cosy shared house a short walk from UP''s Hatfield campus, popular with first- and second-years. Big communal kitchen, quiet study nooks, and a fenced garden.',
  '212 Burnett Street, Hatfield', 'Pretoria', 'Gauteng',
  -25.7482, 28.2367, 1.2, 3800, 3800, 4, 2, 2, true, true,
  ARRAY['WiFi','Furnished','Parking','Kitchen','Backup Power','Water Included'],
  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800', 4.3, 25, 'published'
),
(
  NULL, 'Rondebosch Residences', (SELECT id FROM public.universities WHERE slug = 'uct'),
  'vineyard-view-rondebosch', 'Vineyard View Rondebosch',
  'Bright, modern studio apartments a 10-minute walk from UCT''s upper campus, with views towards Devil''s Peak. Popular with postgrad students who want their own space.',
  '45 Main Road, Rondebosch', 'Cape Town', 'Western Cape',
  -33.9635, 18.4735, 1.5, 7200, 7200, 1, 1, 2, true, true,
  ARRAY['WiFi','Furnished','24/7 Security','Gym','Study Room','Cleaning Service'],
  'https://images.unsplash.com/photo-1502672023488-70e25813eb80?w=800', 4.8, 31, 'published'
),
(
  NULL, 'Stellenbosch Student Stays', (SELECT id FROM public.universities WHERE slug = 'su'),
  'oak-lane-stellenbosch', 'Oak Lane Stellenbosch',
  'Classic Stellenbosch student house on a quiet oak-lined street, walking distance to campus and the town centre. Shared rooms and private rooms both available.',
  '9 Krige Street, Stellenbosch', 'Stellenbosch', 'Western Cape',
  -33.9364, 18.8641, 0.9, 5400, 5400, 5, 2, 3, false, true,
  ARRAY['WiFi','Parking','Kitchen','Study Room','Backup Power'],
  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800', 4.4, 14, 'published'
),
(
  NULL, 'Auckland Park Living', (SELECT id FROM public.universities WHERE slug = 'uj'),
  'kingsway-corner-auckland-park', 'Kingsway Corner Auckland Park',
  'Affordable, secure student accommodation directly across from UJ''s Kingsway campus. NSFAS-friendly pricing with all-inclusive rent.',
  '3 Kingsway Avenue, Auckland Park', 'Johannesburg', 'Gauteng',
  -26.1839, 27.9765, 0.3, 3200, 3200, 1, 1, 4, true, true,
  ARRAY['WiFi','Furnished','24/7 Security','Water Included','Backup Power'],
  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800', 4.1, 22, 'published'
),
(
  NULL, 'Musgrave Student Homes', (SELECT id FROM public.universities WHERE slug = 'ukzn'),
  'ridge-view-musgrave', 'Ridge View Musgrave',
  'Well-maintained shared house close to UKZN Howard College, with easy access to Musgrave''s shops and restaurants. Great option for third-years and postgrads.',
  '77 Ridge Road, Musgrave', 'Durban', 'KwaZulu-Natal',
  -29.8407, 30.9944, 2.1, 3400, 3400, 3, 2, 2, true, false,
  ARRAY['WiFi','Furnished','Parking','Kitchen','Laundry'],
  'https://images.unsplash.com/photo-1484154218962-a197022b5858?w=800', 4.0, 9, 'published'
),
(
  NULL, 'Melville Student Quarters', (SELECT id FROM public.universities WHERE slug = 'wits'),
  'melville-house-wits', 'Melville House',
  'Character-filled house in trendy Melville, a short Uber or bus ride from Wits. Big rooms, a shared lounge with a fireplace, and a rooftop braai area.',
  '21 7th Street, Melville', 'Johannesburg', 'Gauteng',
  -26.1785, 28.0089, 3.4, 4200, 4200, 4, 2, 1, true, true,
  ARRAY['WiFi','Furnished','Parking','Kitchen','Study Room'],
  'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=800', 4.5, 17, 'published'
),
(
  NULL, 'Observatory Student Lofts', (SELECT id FROM public.universities WHERE slug = 'uct'),
  'obs-loft-uct', 'Observatory Student Lofts',
  'Funky loft-style rooms in bohemian Observatory, close to UCT''s medical campus and a quick train ride from upper campus.',
  '58 Lower Main Road, Observatory', 'Cape Town', 'Western Cape',
  -33.9389, 18.4719, 2.8, 4900, 4900, 2, 1, 2, true, true,
  ARRAY['WiFi','Furnished','Kitchen','Study Room','Backup Power'],
  'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?w=800', 4.2, 11, 'published'
)
ON CONFLICT (slug) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Property images (2 extra angles per property, cover_image already set above)
-- ---------------------------------------------------------------------------
-- NOTE: property_images/rooms/reviews have no natural unique constraint for
-- Postgres to use as an ON CONFLICT target (only `id`, which is always a
-- fresh random UUID), so idempotency here is done via NOT EXISTS guards
-- instead of ON CONFLICT DO NOTHING, which would silently do nothing useful
-- on these three tables and let re-running the script duplicate rows.
INSERT INTO public.property_images (property_id, url, alt, position)
SELECT p.id, img.url, img.alt, img.position
FROM public.properties p
CROSS JOIN LATERAL (
  VALUES
    ('https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800', 'Bedroom', 1),
    ('https://images.unsplash.com/photo-1484154218962-a197022b5858?w=800', 'Communal area', 2)
) AS img(url, alt, position)
WHERE p.slug IN (
  'sunset-heights-braamfontein','the-nest-hatfield','vineyard-view-rondebosch','oak-lane-stellenbosch',
  'kingsway-corner-auckland-park','ridge-view-musgrave','melville-house-wits','obs-loft-uct'
)
AND NOT EXISTS (
  SELECT 1 FROM public.property_images existing
  WHERE existing.property_id = p.id AND existing.url = img.url
);

-- ---------------------------------------------------------------------------
-- Rooms per property
-- ---------------------------------------------------------------------------
INSERT INTO public.rooms (property_id, name, room_type, price_per_month, capacity, status)
SELECT p.id, r.name, r.room_type, p.price_per_month + r.price_delta, r.capacity, r.status::public.room_status
FROM public.properties p
CROSS JOIN LATERAL (
  VALUES
    ('Room A', 'single', 0, 1, 'available'),
    ('Room B', 'single', -200, 1, 'available'),
    ('Room C (shared)', 'shared', -600, 2, 'available')
) AS r(name, room_type, price_delta, capacity, status)
WHERE p.slug IN (
  'sunset-heights-braamfontein','the-nest-hatfield','vineyard-view-rondebosch','oak-lane-stellenbosch',
  'kingsway-corner-auckland-park','ridge-view-musgrave','melville-house-wits','obs-loft-uct'
)
AND NOT EXISTS (
  SELECT 1 FROM public.rooms existing
  WHERE existing.property_id = p.id AND existing.name = r.name
);

-- ---------------------------------------------------------------------------
-- Reviews (author_name text field, no linked student account needed)
-- ---------------------------------------------------------------------------
INSERT INTO public.reviews (property_id, author_name, rating, title, body)
SELECT p.id, rev.author_name, rev.rating, rev.title, rev.body
FROM public.properties p
CROSS JOIN LATERAL (
  VALUES
    ('Naledi M.', 5, 'Loved staying here', 'Safe, close to campus, and the landlord responded to maintenance requests within a day.'),
    ('Junaid K.', 4, 'Good value', 'Solid WiFi and the security made me feel comfortable walking in late from the library.')
) AS rev(author_name, rating, title, body)
WHERE p.slug IN (
  'sunset-heights-braamfontein','the-nest-hatfield','vineyard-view-rondebosch','oak-lane-stellenbosch',
  'kingsway-corner-auckland-park','melville-house-wits'
)
AND NOT EXISTS (
  SELECT 1 FROM public.reviews existing
  WHERE existing.property_id = p.id AND existing.author_name = rev.author_name AND existing.title = rev.title
);
