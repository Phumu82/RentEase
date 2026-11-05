-- RentEase DEMO DATA (optional). Run in Supabase -> SQL Editor AFTER the web migrations.
-- Gives guests and students real-looking places to browse, book and review so the app is not empty.
-- * Demo rows are tagged: property slugs end in '-demo' and landlord_id is NULL (no fake users).
-- * Room rents are realistic student rents (e.g. R3400 shared, R5000 bachelor/studio).
-- * Amenity names match the app's search filters exactly (WiFi, 24/7 Security, ...).
-- * Photos: no cover image is set, so the app shows its labelled sample photos. Landlords upload real ones.
-- * Remove it all later with:  delete from public.properties where slug like '%-demo';
-- * Re-running is safe (existing demo rows are skipped).

-- Universities (adds missing ones; for existing ones only refreshes students and average_rent)
insert into public.universities (slug, name, short_name, city, province, students, average_rent) values
  ('university-of-cape-town', 'University of Cape Town', 'UCT', 'Cape Town', 'Western Cape', 29000, 5600),
  ('university-of-the-witwatersrand', 'University of the Witwatersrand', 'Wits', 'Johannesburg', 'Gauteng', 40000, 4300),
  ('university-of-pretoria', 'University of Pretoria', 'UP', 'Pretoria', 'Gauteng', 53000, 3900),
  ('stellenbosch-university', 'Stellenbosch University', 'SU', 'Stellenbosch', 'Western Cape', 32000, 5200),
  ('university-of-kwazulu-natal', 'University of KwaZulu-Natal', 'UKZN', 'Durban', 'KwaZulu-Natal', 47000, 3600)
on conflict (slug) do update set students = excluded.students, average_rent = excluded.average_rent;

-- Properties
insert into public.properties (slug, title, description, address, city, province, university_id, landlord_name, distance_km, price_per_month, deposit, bedrooms, bathrooms, rooms_available, furnished, verified, amenities, status)
select v.slug, v.title, v.descr, v.address, u.city, u.province, u.id, v.landlord, v.dist, v.price, v.price, v.beds, v.baths, v.avail, v.furn, v.ver, v.am, 'published'::public.property_status
from (values
  ('braamfontein-loft-apartments-demo', 'Braamfontein Loft Apartments', 'Renovated loft building a short walk from Wits, with a communal kitchen, fibre internet and backup power during load-shedding.', 'Braamfontein', 'university-of-the-witwatersrand', 'Naledi Student Homes', 0.6, 3400, 4, 2, 2, true, true, array['WiFi','Water Included','24/7 Security','Backup Power']::text[]),
  ('parktown-heights-residence-demo', 'Parktown Heights Residence', 'Purpose-built student residence with biometric access, study room and weekly cleaning of shared areas.', 'Parktown', 'university-of-the-witwatersrand', 'Campus Living Group', 1.4, 3300, 4, 2, 3, true, true, array['WiFi','24/7 Security','Study Room','Cleaning Service','Backup Power']::text[]),
  ('hatfield-garden-rooms-demo', 'Hatfield Garden Rooms', 'Secure complex with covered parking, laundry and a shuttle to campus every 30 minutes.', 'Hatfield', 'university-of-pretoria', 'Van Wyk Residences', 0.8, 3200, 4, 2, 3, true, true, array['WiFi','Water Included','Parking','Laundry','24/7 Security']::text[]),
  ('lynnwood-study-lofts-demo', 'Lynnwood Study Lofts', 'Quiet study-focused lofts with shared kitchen and reliable internet. Short drive to campus.', 'Lynnwood', 'university-of-pretoria', 'Khumalo Property Co.', 3.2, 2900, 3, 1, 2, false, false, array['WiFi','Kitchen','Study Room']::text[]),
  ('rondebosch-corner-flats-demo', 'Rondebosch Corner Flats', 'Corner flats close to the Jammie shuttle route. Furnished rooms with weekly cleaning included.', 'Rondebosch', 'university-of-cape-town', 'Bright Nest Rentals', 1.1, 4200, 4, 2, 2, true, true, array['WiFi','Water Included','Laundry','24/7 Security','Cleaning Service']::text[]),
  ('observatory-creative-quarters-demo', 'Observatory Creative Quarters', 'Characterful shared house in Observatory with a communal kitchen and a creative, student-friendly vibe.', 'Observatory', 'university-of-cape-town', 'Sipho Ndlovu Properties', 2.4, 3800, 3, 1, 2, true, false, array['WiFi','Kitchen','24/7 Security','Backup Power']::text[]),
  ('die-boord-student-village-demo', 'Die Boord Student Village', 'Student village with secure parking, laundry and a shuttle to the Stellenbosch campus.', 'Die Boord', 'stellenbosch-university', 'Van Wyk Residences', 1.8, 3900, 4, 2, 3, true, true, array['WiFi','Water Included','Parking','24/7 Security']::text[]),
  ('eendrag-house-shared-living-demo', 'Eendrag House Shared Living', 'Shared living in the heart of town, five minutes from lectures, with cleaning of common areas.', 'Stellenbosch Central', 'stellenbosch-university', 'Campus Living Group', 0.7, 3600, 3, 1, 1, true, true, array['WiFi','Kitchen','24/7 Security','Study Room','Cleaning Service']::text[]),
  ('glenwood-scholar-suites-demo', 'Glenwood Scholar Suites', 'Scholar suites near Howard College with backup power, secure parking and fibre internet.', 'Glenwood', 'university-of-kwazulu-natal', 'Khumalo Property Co.', 1.2, 2800, 4, 2, 3, true, true, array['WiFi','Water Included','Parking','24/7 Security','Backup Power']::text[]),
  ('musgrave-park-rooms-demo', 'Musgrave Park Rooms', 'Affordable rooms with laundry and a shared kitchen, on the bus route to campus.', 'Musgrave', 'university-of-kwazulu-natal', 'Naledi Student Homes', 2.9, 2700, 3, 1, 2, false, false, array['WiFi','Laundry','Kitchen']::text[])
) as v(slug, title, descr, address, uni_slug, landlord, dist, price, beds, baths, avail, furn, ver, am)
join public.universities u on u.slug = v.uni_slug
on conflict (slug) do nothing;

-- Rooms (only for demo properties that have none yet)
insert into public.rooms (property_id, name, room_type, price_per_month, capacity, status)
select p.id, r.name, r.rtype, r.price, r.cap, r.st::public.room_status
from (values
  ('braamfontein-loft-apartments-demo', 'Shared room A', 'Shared', 3400, 2, 'available'),
  ('braamfontein-loft-apartments-demo', 'Single room B', 'Single', 4200, 1, 'available'),
  ('braamfontein-loft-apartments-demo', 'Studio 3', 'Studio', 5200, 1, 'occupied'),
  ('parktown-heights-residence-demo', 'Shared room 1', 'Shared', 3300, 2, 'available'),
  ('parktown-heights-residence-demo', 'Single room 2', 'Single', 4500, 1, 'available'),
  ('parktown-heights-residence-demo', 'En-suite 3', 'Ensuite', 5400, 1, 'available'),
  ('hatfield-garden-rooms-demo', 'Shared room A', 'Shared', 3200, 2, 'available'),
  ('hatfield-garden-rooms-demo', 'Single room B', 'Single', 3900, 1, 'available'),
  ('hatfield-garden-rooms-demo', 'Bachelor flat C', 'Studio', 5000, 1, 'available'),
  ('lynnwood-study-lofts-demo', 'Shared room A', 'Shared', 2900, 2, 'available'),
  ('lynnwood-study-lofts-demo', 'Single room B', 'Single', 3600, 1, 'available'),
  ('rondebosch-corner-flats-demo', 'Shared room A', 'Shared', 4200, 2, 'available'),
  ('rondebosch-corner-flats-demo', 'Single room B', 'Single', 5500, 1, 'available'),
  ('rondebosch-corner-flats-demo', 'En-suite C', 'Ensuite', 6400, 1, 'occupied'),
  ('observatory-creative-quarters-demo', 'Shared room A', 'Shared', 3800, 2, 'available'),
  ('observatory-creative-quarters-demo', 'Single room B', 'Single', 4900, 1, 'available'),
  ('die-boord-student-village-demo', 'Shared room A', 'Shared', 3900, 2, 'available'),
  ('die-boord-student-village-demo', 'Single room B', 'Single', 5100, 1, 'available'),
  ('die-boord-student-village-demo', 'Studio 4', 'Studio', 6300, 1, 'available'),
  ('eendrag-house-shared-living-demo', 'Shared room A', 'Shared', 3600, 2, 'available'),
  ('eendrag-house-shared-living-demo', 'Single room B', 'Single', 4800, 1, 'reserved'),
  ('glenwood-scholar-suites-demo', 'Shared room A', 'Shared', 2800, 2, 'available'),
  ('glenwood-scholar-suites-demo', 'Single room B', 'Single', 3500, 1, 'available'),
  ('glenwood-scholar-suites-demo', 'Bachelor flat C', 'Studio', 4800, 1, 'available'),
  ('musgrave-park-rooms-demo', 'Shared room A', 'Shared', 2700, 2, 'available'),
  ('musgrave-park-rooms-demo', 'Single room B', 'Single', 3400, 1, 'available')
) as r(slug, name, rtype, price, cap, st)
join public.properties p on p.slug = r.slug
where not exists (select 1 from public.rooms x where x.property_id = p.id);

-- Reviews (3 per verified demo property, only if the property has none yet), then rating/review_count from them
insert into public.reviews (property_id, author_name, rating, title, body)
select p.id, r.author, r.rating, r.title, r.body
from public.properties p
cross join (values
  ('Thandiwe M.', 5, 'Perfect for studying', 'Fast wifi, quiet at night and a short walk to lectures. Landlord responds quickly.'),
  ('Lwazi K.', 4, 'Good value', 'Clean rooms and the shared kitchen is well kept. Load-shedding is handled well.'),
  ('Anele P.', 4, 'Safe and friendly', 'Felt safe coming home late. Would have liked more laundry machines.')
) as r(author, rating, title, body)
where p.slug like '%-demo' and p.verified
  and not exists (select 1 from public.reviews x where x.property_id = p.id);

update public.properties p set
  rating = coalesce((select round(avg(rating)::numeric, 1) from public.reviews r where r.property_id = p.id), 0),
  review_count = (select count(*) from public.reviews r where r.property_id = p.id)
where p.slug like '%-demo';
